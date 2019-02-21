package com.unter

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.navigation.fragment.NavHostFragment
import com.unter.model.UnterAppModel

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [Login.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [Login.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class Login : Fragment() {

    private lateinit var buttonLogin: Button
    private lateinit var buttonBack: FloatingActionButton

    private lateinit var textEmail: EditText
    private lateinit var textPassword: EditText

    private lateinit var model: UnterAppModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProviders.of(activity!!).get(UnterAppModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonLogin = getView()!!.findViewById(R.id.button_login_login)
        buttonBack = getView()!!.findViewById(R.id.fab_login_cancel)

        textEmail = getView()!!.findViewById(R.id.text_login_email)
        textPassword = getView()!!.findViewById(R.id.text_login_password)

        buttonLogin.setOnClickListener {
            model.login(textEmail.text.toString(), textPassword.text.toString())
            NavHostFragment.findNavController(this).navigate(R.id.action_login_to_home)
        }

        buttonBack.setOnClickListener {
            NavHostFragment.findNavController(this).navigateUp()
        }
    }
}
