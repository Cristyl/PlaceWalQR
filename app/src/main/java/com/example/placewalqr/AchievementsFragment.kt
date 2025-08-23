package com.example.placewalqr

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AchievementsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var nicknameTextView: TextView
    private lateinit var emptyMessageTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.achievements_fragment, container, false)

        recyclerView = view.findViewById(R.id.achievementsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        nicknameTextView = view.findViewById(R.id.nicknameTextView)
        emptyMessageTextView = view.findViewById(R.id.emptyMessageTextView)

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

        nickname?.let { nicknameTextView.text = it }

        if (userId == null) {
            showToast("User not logged in")
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.apiService.getPlacesByUser(userId)

                if (response.isSuccessful) {
                    val placesList = response.body() ?: emptyList()

                    if (placesList.isEmpty()) {
                        emptyMessageTextView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyMessageTextView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.adapter = AchievementAdapter(placesList)
                    }

                } else {
                    when (response.code()) {
                        400 -> showToast("User ID parameter is missing")
                        500 -> showToast("Server error, please try again later")
                        else -> showToast("Unknown error occurred")
                    }
                }
            } catch (e: Exception) {
                showToast("Connection error, please check your internet")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
