package com.example.momolabfe.ui.record

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.momolabfe.databinding.FragmentCameraBinding

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_IMAGE_CAPTURE = 101
    private val CAMERA_PERMISSION_CODE = 102

    private lateinit var imageView: ImageView
    private lateinit var openCameraButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        imageView = binding.capturedImageIv
        openCameraButton = binding.openCameraBtn

        openCameraButton.setOnClickListener {
            checkCameraPermission()
        }
    }

    // 카메라 권한 확인 및 요청
    private fun checkCameraPermission() {
        val ctx = requireContext()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한 요청 (Fragment의 requestPermissions 사용)
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            dispatchTakePictureIntent()
        }
    }

    // 카메라로 사진 찍기
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        @Suppress("DEPRECATION")
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }

    // 권한 요청 결과 처리 (Fragment 버전)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            }
        }
    }

    // 찍은 사진 결과 표시 (Fragment 버전)
    @Deprecated("구방식 유지")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == android.app.Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
