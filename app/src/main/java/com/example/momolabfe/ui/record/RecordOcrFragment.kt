package com.example.momolabfe.ui.record

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentRecordOcrBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordOcrFragment : Fragment() {

    private var _binding: FragmentRecordOcrBinding? = null
    private val binding get() = _binding!!

    private var photoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordOcrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        binding.closeIv.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.cameraCv.setOnClickListener {
            checkCameraPermission()
        }

        binding.albumCv.setOnClickListener {
            checkGalleryPermission()
        }
    }

    // 카메라 권한 요청
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        }
    }

    // 갤러리 권한 요청
    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        }
    }

    // 카메라 결과 처리
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri ->
                // 촬영한 이미지를 다음 화면으로 전달
                val fragment = RecordOcrPreviewFragment.newInstance(uri.toString())
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    // 갤러리 결과 처리
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val fragment = RecordOcrPreviewFragment.newInstance(uri.toString())
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkGalleryPermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            else -> {
                galleryPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(photoUri)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}