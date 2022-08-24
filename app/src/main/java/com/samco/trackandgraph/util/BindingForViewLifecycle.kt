package com.samco.trackandgraph.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

//Code from https://medium.com/trade-me/handling-lifecycle-with-view-binding-in-fragments-a7f237c56832

fun <T> Fragment.bindingForViewLifecycle(): ReadWriteProperty<Fragment, T> =
    object : ReadWriteProperty<Fragment, T>, DefaultLifecycleObserver {

        // A backing property to hold our value
        private var binding: T? = null

        init {
            // Observe the View Lifecycle of the Fragment
            this@bindingForViewLifecycle
                .viewLifecycleOwnerLiveData
                .observe(this@bindingForViewLifecycle) {
                    it.lifecycle.addObserver(this)
                }
        }

        //This should be called onDestroyView of the fragment
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            binding = null
        }

        override fun getValue(
            thisRef: Fragment,
            property: KProperty<*>
        ): T {
            // Return the backing property if it's set
            return this.binding!!
        }

        override fun setValue(
            thisRef: Fragment,
            property: KProperty<*>,
            value: T
        ) {
            // Set the backing property
            this.binding = value
        }
    }
