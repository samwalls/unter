package com.unter

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.unter.model.UnterAppModel

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [DriverInfo.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [DriverInfo.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class DriverInfo : Fragment() {

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
        return inflater.inflate(R.layout.fragment_driver_info, container, false)
    }
}
