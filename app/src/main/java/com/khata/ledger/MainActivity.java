package com.khata.ledger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String STATE_TAB = "tab";

    private LinearLayout navHome, navLedger, navStock, navPersonal, navProfile;
    private LinearLayout loadingBox;
    private TextView tvShopName, tvLoading;
    private Button btnRetry;
    private View progress;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        navHome = findViewById(R.id.navHome);
        navLedger = findViewById(R.id.navLedger);
        navStock = findViewById(R.id.navStock);
        navPersonal = findViewById(R.id.navPersonal);
        navProfile = findViewById(R.id.navProfile);
        loadingBox = findViewById(R.id.loadingBox);
        tvShopName = findViewById(R.id.tvShopName);
        tvLoading = findViewById(R.id.tvLoading);
        btnRetry = findViewById(R.id.btnRetry);
        progress = findViewById(R.id.progress);

        if (savedInstanceState != null) currentTab = savedInstanceState.getInt(STATE_TAB, 0);

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTab(0);
            }
        });
        navLedger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTab(1);
            }
        });
        navStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTab(2);
            }
        });
        navPersonal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTab(3);
            }
        });
        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTab(4);
            }
        });
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        DataStore.get().onSyncError = new Cb<String>() {
            @Override
            public void on(String message) {
                Util.toast(MainActivity.this, getString(R.string.syncFailed, message));
            }
        };

        if (DataStore.get().isLoaded()) {
            // e.g. the screen was rotated or the language changed: data is already in memory
            ready(savedInstanceState == null);
        } else {
            loadData();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, currentTab);
    }

    /** Loads the user's document once; the tabs read from DataStore afterwards. */
    private void loadData() {
        loadingBox.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);
        tvLoading.setText(R.string.loading);

        final DataStore ds = DataStore.get();
        ds.load(FirebaseAuth.getInstance().getCurrentUser().getUid(), new Cb<Boolean>() {
            @Override
            public void on(Boolean ok) {
                if (isFinishing() || isDestroyed()) return;
                if (!ok) {
                    progress.setVisibility(View.GONE);
                    tvLoading.setText(R.string.loadFailed);
                    btnRetry.setVisibility(View.VISIBLE);
                    return;
                }
                ds.checkAdmin(new Cb<Boolean>() {
                    @Override
                    public void on(Boolean admin) {
                        if (isFinishing() || isDestroyed()) return;
                        ds.isAdmin = admin;
                        ready(true);
                    }
                });
            }
        });
    }

    private void ready(boolean showTab) {
        loadingBox.setVisibility(View.GONE);
        updateShopName();
        if (showTab) openTab(currentTab);
        else highlight(currentTab);
    }

    public void updateShopName() {
        String shop = DataStore.get().shopName;
        tvShopName.setText(shop == null || shop.isEmpty() ? getString(R.string.appSub) : shop);
    }

    /** Opens the Ledger tab already filtered to one customer (used by the overdue list). */
    public void showLedgerFor(String party) {
        currentTab = 1;
        LedgerFragment fragment = new LedgerFragment();
        Bundle args = new Bundle();
        args.putString(LedgerFragment.ARG_PARTY, party);
        fragment.setArguments(args);
        loadFragment(fragment);
        highlight(1);
    }

    private void openTab(int tab) {
        currentTab = tab;
        Fragment fragment;
        switch (tab) {
            case 1:
                fragment = new LedgerFragment();
                break;
            case 2:
                fragment = new StockFragment();
                break;
            case 3:
                fragment = new PersonalFragment();
                break;
            case 4:
                fragment = new ProfileFragment();
                break;
            default:
                fragment = new HomeFragment();
                break;
        }
        loadFragment(fragment);
        highlight(tab);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    /** Gold icon + label for the open tab, white for the others. */
    private void highlight(int tab) {
        LinearLayout[] tabs = {navHome, navLedger, navStock, navPersonal, navProfile};
        int gold = ContextCompat.getColor(this, R.color.goldLeaf);
        int white = ContextCompat.getColor(this, R.color.white);
        for (int i = 0; i < tabs.length; i++) {
            ImageView icon = (ImageView) tabs[i].getChildAt(0);
            TextView label = (TextView) tabs[i].getChildAt(1);
            int color = i == tab ? gold : white;
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(color));
            label.setTextColor(color);
        }
    }

    public void signOut() {
        DataStore.get().flush();
        FirebaseAuth.getInstance().signOut();
        try {
            LoginActivity.googleClient(this).signOut();
        } catch (Exception ignored) {
        }
        DataStore.reset();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) DataStore.get().flush();
    }
}
