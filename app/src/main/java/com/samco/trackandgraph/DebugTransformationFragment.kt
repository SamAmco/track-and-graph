package com.samco.trackandgraph

import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.blacksquircle.ui.editorkit.model.EditorConfig
import com.blacksquircle.ui.editorkit.widget.TextProcessor
import com.blacksquircle.ui.language.python.PythonLanguage
import com.samco.trackandgraph.antlr.TnGLanguage
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.entity.DataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DebugTransformationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DebugTransformationFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val res_view = inflater.inflate(R.layout.fragment_debug_transformation, container, false)

        val editor = res_view.findViewById<TextProcessor>(R.id.editor)
        editor.language = TnGLanguage() // or any other language you want
        editor.editorConfig = EditorConfig(
            fontSize = 16f, // text size, including the line numbers
            fontType = Typeface.MONOSPACE, // typeface, including the line numbers

            wordWrap = true, // whether the word wrap enabled
            codeCompletion = true, // whether the code suggestions will shown
            pinchZoom = true, // whether the zoom gesture enabled
            lineNumbers = true, // line numbers visibility
            highlightCurrentLine = true, // whether the current line will be highlighted
            highlightDelimiters = true, // highlight open/closed brackets beside the cursor

            softKeyboard = true, // whether the fullscreen editing keyboard will shown

            autoIndentation = true, // whether the auto indentation enabled
            autoCloseBrackets = true, // automatically close open parenthesis/bracket/brace
            autoCloseQuotes = true, // automatically close single/double quote when typing
            useSpacesInsteadOfTabs = true, // insert spaces instead of tabs when using auto-indentation
            tabWidth = 4 // the tab width, works together with `useSpacesInsteadOfTabs`
        )




        val application = requireActivity().application
        val dao = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao

//        val all_features = get_all_features(requireActivity().application)

//        val datapointsLive = dao.getDataPointsForFeature(0)

//        val datapoints = mutableListOf<DataPoint>()
//        dao.getDataPointsForFeature(0).observe(viewLifecycleOwner,
//            {it : List<DataPoint> -> it.iterator().forEach { datapoints.add(it)} } )

        editor?.setTextContent("HEELO WLRF.\n\nhiiii!")

        val checkButton = res_view.findViewById<Button>(R.id.button1)
        checkButton.setOnClickListener(
            { editor?.language?.getParser()?.execute("does it matter?", editor?.text.toString()) }
        )

//        editor?.parse()
        editor?.setErrorLine(1)
        editor?.setError("WLRF")




        return res_view



    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DebugTransformationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DebugTransformationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}

//private suspend fun get_all_features(application: TrackAndGraphApplication) {
//    withContext(Dispatchers.IO) {
////        val application = requireActivity().application
//        val dao = TrackAndGraphDatabase.getInstance(application).trackAndGraphDatabaseDao
//
//
//        val all_features = dao.getAllFeaturesAndTrackGroupsSync()
//        val test = 5
//        return@withContext all_features
//    }
//
//}