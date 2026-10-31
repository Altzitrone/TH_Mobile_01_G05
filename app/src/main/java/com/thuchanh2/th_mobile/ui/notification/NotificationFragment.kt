package com.thuchanh2.th_mobile.ui.notification

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.databinding.FragmentNotificationBinding
import com.thuchanh2.th_mobile.ui.showError
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class NotificationFragment : Fragment(R.layout.fragment_notification) {
    private val viewModel: NotificationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentNotificationBinding.bind(view)
        val adapter = NotificationAdapter { item ->
            viewLifecycleOwner.lifecycleScope.launch { viewModel.markAsRead(item.id) }
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter
        binding.chipGroupNotif.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setFilterByChipId(ids.firstOrNull() ?: R.id.chipAllNotif)
        }
        binding.btnMarkAllRead.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try { viewModel.markAllAsRead() }
                catch (error: CancellationException) { throw error }
                catch (error: Exception) { (activity as? AppCompatActivity)?.showError(error) }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notifications.collect {
                    adapter.submitList(it)
                    binding.rvNotifications.isVisible = it.isNotEmpty()
                    binding.layoutEmptyNotif.isVisible = it.isEmpty()
                }
            }
        }
    }
}
