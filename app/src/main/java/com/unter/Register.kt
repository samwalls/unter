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
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.unter.model.UnterAppModel
import com.unter.model.exception.RegisterException

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [Register.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [Register.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class Register : Fragment() {

    private lateinit var textEmail: EditText
    private lateinit var textPassword: EditText

    private lateinit var buttonRegister: Button
    private lateinit var buttonBack: FloatingActionButton

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
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // set up UI components
        buttonRegister = view.findViewById(R.id.button_register_register)
        buttonBack = view.findViewById(R.id.fab_register_cancel)
        textEmail = view.findViewById(R.id.text_register_email)
        textPassword = view.findViewById(R.id.text_register_password)


        buttonRegister.setOnClickListener {
            try {
                // request the controller to register a user
                model.register(textEmail.text.toString(), textPassword.text.toString())
                // change the view if successful
                NavHostFragment.findNavController(this).navigate(R.id.action_register_to_login)
            } catch (e: RegisterException) {
                // display error if unsuccessful
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }

        buttonBack.setOnClickListener {
            // move back on the navigation stack
            NavHostFragment.findNavController(this).navigateUp()
        }
    }
}
