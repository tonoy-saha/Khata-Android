package com.khata.ledger;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {
    private TextView tvEmail;
    private EditText etShopName;
    private Button btnLanguage, btnAdmin, btnSignOut;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        tvEmail = view.findViewById(R.id.tvEmail);
        etShopName = view.findViewById(R.id.etShopName);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        btnAdmin = view.findViewById(R.id.btnAdmin);
        btnSignOut = view.findViewById(R.id.btnSignOut);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) tvEmail.setText(user.getEmail());

        DataStore ds = DataStore.get();
        etShopName.setText(ds.shopName);
        Util.watch(etShopName, new Runnable() {
            @Override
            public void run() {
                DataStore store = DataStore.get();
                String name = etShopName.getText().toString().trim();
                if (!name.equals(store.shopName)) {
                    store.shopName = name;
                    store.save();
                    ((MainActivity) requireActivity()).updateShopName();
                }
            }
        });

        btnAdmin.setVisibility(ds.isAdmin ? View.VISIBLE : View.GONE);
        btnAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), AdminActivity.class));
            }
        });
        btnLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean bangla = getResources().getConfiguration().getLocales().get(0).getLanguage().equals("bn");
                // AppCompat saves the choice and recreates the screens in the new language.
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(bangla ? "en" : "bn"));
            }
        });
        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) requireActivity()).signOut();
            }
        });
        return view;
    }
}
