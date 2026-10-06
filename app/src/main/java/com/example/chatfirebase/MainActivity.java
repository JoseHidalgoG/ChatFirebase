package com.example.chatfirebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.chatfirebase.data.repository.ProfileRepository;
import com.example.chatfirebase.databinding.ActivityMainBinding;
import com.example.chatfirebase.ui.auth.LoginActivity;
import com.example.chatfirebase.ui.main.MainViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private final ProfileRepository profileRepository = new ProfileRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(
                binding.main,
                (view, windowInsets) -> {
                    Insets systemBars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return windowInsets;
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

        NavController navController = navHostFragment.getNavController();


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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_logout) {

            new ViewModelProvider(this)
                    .get(MainViewModel.class)
                    .logout();

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
}