package com.example.yanyee.iotpet;

import android.app.Fragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnFragmentInteractionListener,SummaryFragment.OnFragmentInteractionListener{


    NavigationView navigationView;
    DrawerLayout drawerLayOut;
    FragmentManager fragmentManager;
    private String[] contents = {"HomeFragment", "SummaryFragment"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayOut = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nv_View);

        //Starting Page HomeFragment
        fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, new HomeFragment()).commit();

        setupDrawerContent(navigationView);
    }


    public void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {

                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        selectDrawerItem(item);
                        return true;
                    }
                }
                //item refers to the item you have selected on.
        );
    }

    public void selectDrawerItem(MenuItem item) {
        Fragment fragment;
        switch(item.getItemId()){
            case R.id.nav_first_fragment:
                fragment = new HomeFragment();
                Toast.makeText(this, "HomeFragment Selected", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_second_fragment:
                fragment = new SummaryFragment();
                Toast.makeText(this, "SummaryFragment Selected", Toast.LENGTH_SHORT).show();
                break;

            default:
                fragment = new HomeFragment();
        }

        fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        item.setChecked(true);
        setTitle(item.getTitle());
        drawerLayOut.closeDrawers();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the HomeFragment/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
