package com.example.placewalqr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import android.widget.Toast


class AchievementsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var nicknameTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.achievements_activity, container, false)

        recyclerView = view.findViewById(R.id.achievementsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        nicknameTextView = view.findViewById(R.id.nicknameTextView)

        fetchAchievements()

        return view
    }

    private fun fetchAchievements() {
        val sharedPreferences = requireActivity().getSharedPreferences(
            "UserPrefs",
            android.content.Context.MODE_PRIVATE
        )
        val userId = sharedPreferences.getString("id", null)
        val nickname = sharedPreferences.getString("nickname", null)

        nickname?.let {
            nicknameTextView.text = it
        }

        if (userId != null) {
            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.apiService.getPlacesByUser(userId)
                    if (response.isSuccessful) {
                        val placesList = response.body() ?: emptyList()
                        recyclerView.adapter = AchievementAdapter(placesList)
                    } else {
                        showToast("Errore nel caricamento dei premi")
                    }
                } catch (e: Exception) {
                    showToast("Errore di connessione")
                }
            }
        } else {
            showToast("Utente non loggato")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
