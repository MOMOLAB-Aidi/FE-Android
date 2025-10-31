package com.example.momolabfe.ui.record

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentCameraBinding
import java.io.File

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var capturedUriString: Uri? = null // 촬영한 이미지를 파일로 저장하고 얻은 URI 보관

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.openCameraBtn.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        binding.nextTv.setOnClickListener {
            val uri = capturedUriString

            val next = RecordExchangeListFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("image_uri", uri)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, next)
                .addToBackStack(null)
                .commit()
        }
    }

    // 카메라 권한 런처
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePicturePreviewLauncher.launch(null)
            } else {
                parentFragmentManager.popBackStack()
            }
        }

    // 카메라 미리보기 촬영 런처 (Bitmap 썸네일 반환)
    private val takePicturePreviewLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap ?: return@registerForActivityResult

            binding.capturedImageIv.setImageBitmap(bitmap) // 미리보기 표시

            // 캐시 폴더에 저장
            val ctx = requireContext()
            val dir = File(ctx.cacheDir, "images").apply { mkdirs() }
            val file = File.createTempFile("thumb_", ".jpg", dir)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // FileProvider로 content:// URI 생성
            val uri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )
            capturedUriString = uri
            binding.nextTv.visibility = View.VISIBLE
        }

    // 카메라 권한 확인 및 요청
    private fun checkCameraPermissionAndLaunch() {
        val ctx = requireContext()
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            takePicturePreviewLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
