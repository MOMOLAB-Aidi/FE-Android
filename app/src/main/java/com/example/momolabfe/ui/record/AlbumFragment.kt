package com.example.momolabfe.ui.record

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.momolabfe.R
import com.example.momolabfe.databinding.FragmentAlbumBinding
import com.example.momolabfe.ui.record.adapter.AlbumAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlbumFragment : Fragment() {

    private var _binding: FragmentAlbumBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AlbumAdapter

    private var selectedUri: Uri? = null // 선택한 이미지 URI 보관

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 바텀 내비게이션 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.main_bnv)?.visibility = View.GONE

        adapter = AlbumAdapter { uri ->
            selectedUri = uri
            showPreview(uri)
        }
        binding.photosRv.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.photosRv.adapter = adapter

        checkPermissionAndLoad()

        binding.cancelTv.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.nextTv.setOnClickListener {
            val uri = selectedUri

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

    // Android 13+ 은 READ_MEDIA_IMAGES, 이하 버전은 READ_EXTERNAL_STORAGE
    private val requiredPermissions: Array<String> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // 갤러리 접근 권한 요청 런처
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                loadAndShowImages()
            } else {
                parentFragmentManager.popBackStack()
            }
        }

    // 권한 확인 및 이미지 로드
    private fun checkPermissionAndLoad() {
        val ctx = requireContext()
        val notGranted = requiredPermissions.any {
            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            loadAndShowImages()
        }
    }

    private fun loadAndShowImages() {
        val imageUris = loadImageUrisFromMediaStore()
        adapter.submitList(imageUris)

        if (imageUris.isEmpty()) {
            binding.imageNotExistsCv.isVisible = true
            binding.nextTv.isVisible = false
        } else {
            binding.imageNotExistsCv.isVisible = false
            selectedUri = imageUris.first()
            showPreview(selectedUri!!)
            binding.nextTv.isVisible = true
        }
    }

    // MediaStore에서 최신순으로 이미지 URI 가져오기
    private fun loadImageUrisFromMediaStore(limit: Int = 300): List<Uri> {
        val uris = mutableListOf<Uri>()

        val collection: Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }
        requireContext().contentResolver.query(collection, projection, args, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                uris.add(contentUri)
            }
        }

        return uris
    }

    // 클릭 시 항상 초기화 -> 로드 -> 재배치
    private fun showPreview(uri: Uri) {
        val iv = binding.previewIv

        Glide.with(this)
            .load(uri)
            .dontTransform()
            .dontAnimate()
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    val w = resource.intrinsicWidth
                    val h = resource.intrinsicHeight
                    iv.applyFitAndCenter(w, h)
                    iv.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    iv.setImageDrawable(placeholder)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
