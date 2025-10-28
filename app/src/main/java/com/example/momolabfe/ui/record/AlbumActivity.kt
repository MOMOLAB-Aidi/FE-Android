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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.momolabfe.databinding.ActivityAlbumBinding
import com.example.momolabfe.ui.record.adapter.AlbumAdapter
import com.google.android.material.snackbar.Snackbar

class AlbumActivity : AppCompatActivity() {

    private var _binding: ActivityAlbumBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AlbumAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AlbumAdapter { uri ->
            showPreview(uri)
        }
        binding.photosRv.layoutManager = GridLayoutManager(this, 3)
        binding.photosRv.adapter = adapter

        checkPermissionAndLoad()

        binding.cancelTv.setOnClickListener {
            finish()
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
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                loadAndShowImages()
            } else {
                finish()
            }
        }

    // 권한 확인 및 이미지 로드
    private fun checkPermissionAndLoad() {
        val notGranted = requiredPermissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            loadAndShowImages()
        }
    }

    private fun loadAndShowImages() {
        // 미디어 스토어에서 이미지 URI 리스트 조회
        val imageUris = loadImageUrisFromMediaStore()
        adapter.submitList(imageUris)

        if (imageUris.isEmpty()) {
            Snackbar.make(binding.root, "표시할 이미지가 없습니다.", Snackbar.LENGTH_SHORT).show()
        }  else {
            showPreview(imageUris.first())
        }
    }

    // MediaStore에서 최신순으로 이미지 URI 가져오기
    private fun loadImageUrisFromMediaStore(limit: Int = 300): List<Uri> {
        val uris = mutableListOf<Uri>()

        // Android API 29+ 에서는 VOLUME_EXTERNAL 사용
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
        contentResolver.query(collection, projection, args, null)?.use { cursor ->
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
                    // 리소스 크기로 먼저 매트릭스 계산 & 적용
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


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
