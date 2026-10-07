package com.example.chatfirebase;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.chatfirebase.data.repository.FcmTokenRepository;
import com.example.chatfirebase.data.repository.ProfileRepository;
import com.example.chatfirebase.databinding.ActivityMainBinding;
import com.example.chatfirebase.notifications.NotificationHelper;
import com.example.chatfirebase.ui.auth.LoginActivity;
import com.example.chatfirebase.ui.main.MainViewModel;
import com.example.chatfirebase.util.Constants;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private MainViewModel mainViewModel;
    private final ProfileRepository profileRepository = new ProfileRepository();

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> { }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        if (!mainViewModel.isLoggedIn()) {
            openLogin();
            return;
        }

        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(
                binding.main,
                (view, windowInsets) -> {
                    Insets systemBars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

                    boolean keyboardOpen = ime.bottom > 0;
                    boolean bottomNavShown = binding.bottomNav.getVisibility() == View.VISIBLE;


                    int rootBottom = keyboardOpen
                            ? ime.bottom
                            : (bottomNavShown ? 0 : systemBars.bottom);

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            rootBottom
                    );

                    binding.bottomNav.setPadding(
                            0,
                            0,
                            0,
                            bottomNavShown && !keyboardOpen ? systemBars.bottom : 0
                    );

                    return WindowInsetsCompat.CONSUMED;
                }
        );

        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            throw new IllegalStateException(
                    "No se encontró el NavHostFragment."
            );
        }

        navController = navHostFragment.getNavController();


        navController.addOnDestinationChangedListener(
                (controller, destination, arguments) -> {
                    int destinationId = destination.getId();

                    if (destinationId == R.id.chatsFragment) {
                        binding.toolbar.setTitle("Chats");
                    } else if (destinationId == R.id.contactsFragment) {
                        binding.toolbar.setTitle("Contactos");
                    } else if (destinationId == R.id.profileFragment) {
                        binding.toolbar.setTitle("Perfil");
                    }

                    // Como en otras apps de mensajería, la conversación ocupa toda la pantalla.
                    binding.bottomNav.setVisibility(
                            destinationId == R.id.chatFragment ? View.GONE : View.VISIBLE
                    );
                    ViewCompat.requestApplyInsets(binding.main);
                }
        );

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.chatsFragment,
                R.id.contactsFragment,
                R.id.profileFragment
        ).build();

        NavigationUI.setupActionBarWithNavController(
                this,
                navController,
                appBarConfiguration
        );

        NavigationUI.setupWithNavController(
                binding.bottomNav,
                navController
        );

        // Notificaciones: canal y token FCM al que la Cloud Function envía los pushes.
        NotificationHelper.ensureChannel(this);
        new FcmTokenRepository().registerCurrentToken();

        if (savedInstanceState == null) {
            requestNotificationPermissionIfNeeded();
            openChatFromIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openChatFromIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_logout) {

            mainViewModel.logout();
            openLogin();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            return NavigationUI.navigateUp(
                    navHostFragment.getNavController(),
                    appBarConfiguration
            );
        }

        return super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        profileRepository.updatePresence(true);
    }

    @Override
    protected void onStop() {
        profileRepository.updatePresence(false);
        super.onStop();
    }

    private void openLogin() {
        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** Si el Intent viene de una notificación de mensaje, abre esa conversación. */
    private void openChatFromIntent(@Nullable Intent intent) {
        if (intent == null || navController == null) {
            return;
        }

        String chatId = intent.getStringExtra(Constants.EXTRA_CHAT_ID);

        if (chatId == null || chatId.trim().isEmpty()) {
            return;
        }

        // Los argumentos de chatFragment no admiten null.
        String otherUserId = intent.getStringExtra(Constants.EXTRA_OTHER_USER_ID);
        String otherUserName = intent.getStringExtra(Constants.EXTRA_OTHER_USER_NAME);

        Bundle args = new Bundle();
        args.putString(Constants.EXTRA_CHAT_ID, chatId);
        args.putString(Constants.EXTRA_OTHER_USER_ID, otherUserId != null ? otherUserId : "");
        args.putString(Constants.EXTRA_OTHER_USER_NAME, otherUserName != null ? otherUserName : "");

        // Atrás vuelve a la lista de chats y no se apilan conversaciones.
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.chatsFragment, false)
                .build();

        navController.navigate(R.id.chatFragment, args, options);

        // Evita reabrir el chat si la actividad se recrea con este mismo Intent.
        intent.removeExtra(Constants.EXTRA_CHAT_ID);
    }
}
