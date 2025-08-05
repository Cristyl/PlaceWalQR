package com.example.kotlindemos

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

object PermissionUtils {
    //check if a permission has been granted
    fun isPermissionGranted(context: Context, permission: String): Boolean{
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isPermissionGranted(permissions: Array<String>, grantResult: IntArray, permission: String): Boolean{
        for (index in permissions.indices){
            if(permissions[index]==permission){
                return grantResult.isNotEmpty() && grantResult[index] == PackageManager.PERMISSION_GRANTED
            }
        }
        return false
    }

    //check if location permission has been granted
    fun isLocationPermissionGranted(context: Context): Boolean{
        return isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    //dialog for showing if a permission is denied
    class PermissionDeniedDialog: DialogFragment(){
        private var finishActivity=false

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val title=arguments?.getString(ARGUMENT_TITLE)?:"Permission denied"
            val message=arguments?.getString(ARGUMENT_MESSAGE)?:"Permission is required"
            finishActivity=arguments?.getBoolean(ARGUMENT_FINISH_ACTIVITY, false)?:false

            return AlertDialog.Builder(requireActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK"){_, _->
                    if(finishActivity){
                        requireActivity().finish()
                    }
                }
                .create()
        }

        companion object{
            private const val ARGUMENT_TITLE="title"
            private const val ARGUMENT_MESSAGE="message"
            private const val ARGUMENT_FINISH_ACTIVITY="finish"

            //create dialog instace
            fun newInstance(title: String, message: String, finishActivity: Boolean=false): PermissionDeniedDialog{
                val dialog= PermissionDeniedDialog()
                val args=Bundle().apply {
                    putString(ARGUMENT_TITLE, title)
                    putString(ARGUMENT_MESSAGE, message)
                    putBoolean(ARGUMENT_FINISH_ACTIVITY, finishActivity)
                }
                dialog.arguments=args
                return dialog
            }
        }
    }
}