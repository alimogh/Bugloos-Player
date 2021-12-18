package org.hfathi.bugloos.loading

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.hfathi.bugloos.R
import org.hfathi.bugloos.databinding.FragmentLoadingBinding
import org.hfathi.bugloos.logD
import org.hfathi.bugloos.music.MusicStore

/**
 * Fragment that handles what to display during the loading process.
 * @author hamid fathi
 */
class LoadingFragment : Fragment() {
    private val loadingModel: LoadingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLoadingBinding.inflate(inflater)

        // Build the permission launcher here as you can only do it in onCreateView/onCreate
        val permLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(), ::onPermResult
        )

        // --- UI SETUP ---

        binding.loadingModel = loadingModel

        // --- VIEWMODEL SETUP ---

        loadingModel.doGrant.observe(viewLifecycleOwner) { doGrant ->
            if (doGrant) {
                permLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                loadingModel.doneWithGrant()
            }
        }

        loadingModel.response.observe(viewLifecycleOwner) { response ->
            when (response) {
                // Success should lead to navigation to the main fragment
                MusicStore.Response.SUCCESS -> findNavController().navigate(
                    LoadingFragmentDirections.actionToMain()
                )

                // Null means that the loading process is going on
                null -> showLoading(binding)

                // Anything else is an error
                else -> showError(binding, response)
            }
        }

        if (hasNoPermissions()) {
            // MusicStore.Response.NO_PERMS isnt actually returned by MusicStore, its just
            // a way to keep the current permission state across device changes
            loadingModel.notifyNoPermissions()
        }

        if (loadingModel.response.value == null) {
            loadingModel.load(requireContext())
        }

        logD("Fragment created")

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // Navigate away if the music has already been loaded.
        // This causes a memory leak, but there's nothing I can do about it.
        if (loadingModel.loaded) {
            findNavController().navigate(
                LoadingFragmentDirections.actionToMain()
            )
        }
    }

    // --- PERMISSIONS ---

    /**
     * Check if bugloos_player has the permissions to load music
     */
    private fun hasNoPermissions(): Boolean {
        val needRationale = shouldShowRequestPermissionRationale(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val notGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_DENIED

        return needRationale || notGranted
    }

    private fun onPermResult(granted: Boolean) {
        if (granted) {
            // If granted, its now safe to load, which will clear the NO_PERMS response
            // we applied earlier.
            loadingModel.load(requireContext())
        }
    }

    // --- UI DISPLAY ---

    /**
     * Hide all error elements and return to the loading view
     */
    private fun showLoading(binding: FragmentLoadingBinding) {
        binding.apply {
            loadingErrorIcon.visibility = View.GONE
            loadingErrorText.visibility = View.GONE
            loadingRetryButton.visibility = View.GONE
            loadingGrantButton.visibility = View.GONE
            loadingCircle.visibility = View.VISIBLE
        }
    }

    /**
     * Show an error prompt.
     * @param error The [MusicStore.Response] that this error corresponds to. Ignores
     * [MusicStore.Response.SUCCESS]
     */
    private fun showError(binding: FragmentLoadingBinding, error: MusicStore.Response) {
        binding.loadingCircle.visibility = View.GONE
        binding.loadingErrorIcon.visibility = View.VISIBLE
        binding.loadingErrorText.visibility = View.VISIBLE

        when (error) {
            MusicStore.Response.NO_MUSIC -> {
                binding.loadingRetryButton.visibility = View.VISIBLE
                binding.loadingErrorText.text = getString(R.string.error_no_music)
            }

            MusicStore.Response.NO_PERMS -> {
                binding.loadingGrantButton.visibility = View.VISIBLE
                binding.loadingErrorText.text = getString(R.string.error_no_perms)
            }

            MusicStore.Response.FAILED -> {
                binding.loadingRetryButton.visibility = View.VISIBLE
                binding.loadingErrorText.text = getString(R.string.error_load_failed)
            }

            else -> {}
        }
    }
}
