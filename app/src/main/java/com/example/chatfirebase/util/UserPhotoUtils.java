
package com.example.chatfirebase.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;

import com.example.chatfirebase.R;

public final class UserPhotoUtils {

    private UserPhotoUtils() {
    }

    public static void loadPhoto(
            ImageView imageView,
            String photoBase64,
            String photoUrl
    ) {
        // Imagen predeterminada mientras no haya una foto valida
        imageView.setImageResource(R.drawable.ic_person);

        if (photoBase64 != null && !photoBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(
                        photoBase64,
                        Base64.DEFAULT
                );

                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        bytes, 0, bytes.length
                );

                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            } catch (IllegalArgumentException ignored) {
                // Si Base64 no es válido, probamos con la URL.
            }
        }

        if (photoUrl != null && !photoUrl.trim().isEmpty()) {
            com.bumptech.glide.Glide.with(imageView)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(imageView);
        }
    }
}