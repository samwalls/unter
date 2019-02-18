package com.unter.activity

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.unter.Login
import com.unter.R
import com.unter.Register
import com.unter.Splash

class MainActivity : AppCompatActivity(),
    Splash.OnFragmentInteractionListener,
    Login.OnFragmentInteractionListener,
    Register.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
