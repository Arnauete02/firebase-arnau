package com.example.firebasetemplate;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.firebasetemplate.databinding.FragmentNewPostBinding;
import com.example.firebasetemplate.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.time.LocalDateTime;
import java.util.UUID;

public class NewPostFragment extends AppFragment {

    private FragmentNewPostBinding binding;
    private Uri uriImg;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return (binding = FragmentNewPostBinding.inflate(inflater, container, false)).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.previsualizacion.setImageAlpha(R.drawable.ic_baseline_add_a_photo_24);
        binding.previsualizacion.setOnClickListener(v -> seleccionarImagen());

        appViewModel.uriImagenSeleccionada.observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                Glide.with(this).load(uri).into(binding.previsualizacion);
                uriImg = uri;
            }
        });

        binding.publicar.setOnClickListener(v -> {
            if (uriImg != null) {
                binding.publicar.setEnabled(false);

                FirebaseStorage.getInstance()
                        .getReference("/images/" + UUID.randomUUID() + ".jpg")
                        .putFile(uriImg)
                        .continueWithTask(task -> task.getResult().getStorage().getDownloadUrl())
                        .addOnSuccessListener(urlDescarga -> {
                            Post post = new Post();
                            post.content = binding.contenido.getText().toString();
                            post.authorName = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                            post.date = LocalDateTime.now().toString();
                            post.imageUrl = urlDescarga.toString();

                            FirebaseFirestore.getInstance()
                                    .collection("posts")
                                    .add(post)
                                    .addOnCompleteListener(task -> {
                                        appViewModel.setUriImagenSeleccionada(null);
                                        binding.publicar.setEnabled(true);
                                        navController.popBackStack();
                                    });
                        });
            } else {
                Toast.makeText(getContext(), "Puja una imatge", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void seleccionarImagen() {
        galeria.launch("image/*");
    }

    private final ActivityResultLauncher<String> galeria = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        appViewModel.setUriImagenSeleccionada(uri);
    });
}