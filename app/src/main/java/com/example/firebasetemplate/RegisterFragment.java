package com.example.firebasetemplate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.firebasetemplate.databinding.FragmentRegisterBinding;
import com.example.firebasetemplate.model.Post;
import com.example.firebasetemplate.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.time.LocalDateTime;
import java.util.UUID;


public class RegisterFragment extends AppFragment {
    private FragmentRegisterBinding binding;
    private Uri uriImg;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return (binding = FragmentRegisterBinding.inflate(inflater, container, false)).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.imageRegister.setOnClickListener(v -> seleccionarImagen());

        appViewModel.uriImagenSeleccionada.observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                Glide.with(this).load(uri).into(binding.imageRegister);
                uriImg = uri;
            }
        });

        binding.createAccountButton.setOnClickListener(v -> {
            if (binding.emailEditText.getText().toString().isEmpty() ) {
                binding.emailEditText.setError("Required");
                return;
            }
            if (binding.passwordEditText.getText().toString().isEmpty() ) {
                binding.passwordEditText.setError("Required");
                return;
            }
            if (binding.usernameEditText.getText().toString().isEmpty() ) {
                binding.usernameEditText.setError("Required");
                return;
            }
            if (uriImg != null) {
                FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(
                                binding.emailEditText.getText().toString(),
                                binding.passwordEditText.getText().toString()
                        )
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(binding.usernameEditText.getText().toString())
                                            .setPhotoUri(uriImg)
                                            .build();
                                    user.updateProfile(profileUpdates)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        navController.navigate(R.id.action_registerFragment_to_postsHomeFragment);
                                                    } else {
                                                        Toast.makeText(requireContext(), task.getException().getLocalizedMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } else {
                                    Log.w("FAIL", "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(requireContext(), task.getException().getLocalizedMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                FirebaseStorage.getInstance()
                        .getReference("/images/" + UUID.randomUUID() + ".jpg")
                        .putFile(uriImg)
                        .continueWithTask(task -> task.getResult().getStorage().getDownloadUrl())
                        .addOnSuccessListener(urlDescarga -> {
                            User user = new User();
                            user.username = binding.usernameEditText.getText().toString();
                            user.email = binding.emailEditText.getText().toString();
                            user.imageUrl = urlDescarga.toString();

                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .add(user);
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