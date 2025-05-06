package com.secret.blackholeglow.activities;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.secret.blackholeglow.R;
import com.secret.blackholeglow.fragments.AnimatedWallpaperListFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 2. Configurar DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // 3. Botón de menú hamburguesa
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 4. Selección por defecto
        if (savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.nav_animated);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AnimatedWallpaperListFragment())
                    .commit();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Por ahora, reutilizamos el mismo fragmento como placeholder
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AnimatedWallpaperListFragment()) // temporal
                    .commit();
        } else if (id == R.id.nav_animated) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AnimatedWallpaperListFragment())
                    .commit();
        } else if (id == R.id.nav_favorites) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AnimatedWallpaperListFragment()) // temporal
                    .commit();
        } else if (id == R.id.nav_settings) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AnimatedWallpaperListFragment()) // temporal
                    .commit();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}