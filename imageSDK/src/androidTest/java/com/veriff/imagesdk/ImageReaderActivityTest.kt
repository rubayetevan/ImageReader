package com.veriff.imagesdk


import android.view.View
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import com.veriff.imagesdk.util.RecognizeType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class ImageReaderActivityTest {

    private lateinit var imageReaderActivity: ImageReaderActivity

    @get:Rule
    var activityScenarioRule = activityScenarioRule<ImageReaderActivity>()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            imageReaderActivity = it
        }
    }

    @Test
    fun testImageCaptureButton() {
        val view: View = imageReaderActivity.findViewById(R.id.image_capture_button)
        assertNotNull(view)
    }

    @Test
    fun testPreview() {
        val view: View = imageReaderActivity.findViewById(R.id.viewFinder)
        assertNotNull(view)
    }

    @Test
    fun testProgressbar() {
        val view: View = imageReaderActivity.findViewById(R.id.progressBar)
        assertNotNull(view)
    }

    @Test
    fun testProgressText() {
        val view: View = imageReaderActivity.findViewById(R.id.textView)
        assertNotNull(view)
    }

    /*
    * This test needs a real device to run.
    * Please face the rear camera to any latin text.
    * Do not face the rear camera towards blank space where no text available.
    */
    @Test
    fun testTextResultPositive() {
        ImageReaderActivity.recognizeType = RecognizeType.TEXT
        launchActivity<ImageReaderActivity>().use { it ->
            onView(withId(R.id.image_capture_button)).perform(click())

            val resultCode = it.result.resultCode
            assertEquals(ImageReaderActivity.RESULT_SUCCESS, resultCode)

            val resultData = it.result.resultData

            val type =  resultData.getStringExtra(ImageReaderActivity.KEY_RECOGNIZE_TYPE)
            type?.let { t->
                assertEquals(RecognizeType.TEXT,RecognizeType.valueOf(t))
            }?: run {
                assertNotNull(type)
            }

            val data = resultData.getStringExtra(ImageReaderActivity.KEY_DATA)
            data?.let { d ->
                assertTrue(d.isNotEmpty() && d.isNotBlank())
            }?:run {
                assertNotNull(data)
            }
        }
    }

    /*
    * This test needs a real device to run.
    * Do not face the rear camera to any text.
    * Face the rear camera towards blank space where no text is available.
    */
    @Test
    fun testTextResultNegative() {
        ImageReaderActivity.recognizeType = RecognizeType.TEXT
        launchActivity<ImageReaderActivity>().use { it ->
            onView(withId(R.id.image_capture_button)).perform(click())

            val resultCode = it.result.resultCode
            assertEquals(ImageReaderActivity.RESULT_ERROR, resultCode)

            val resultData = it.result.resultData

            val type =  resultData.getStringExtra(ImageReaderActivity.KEY_RECOGNIZE_TYPE)
            type?.let { t->
                assertEquals(RecognizeType.TEXT,RecognizeType.valueOf(t))
            }?: run {
                assertNotNull(type)
            }

            val data = resultData.getStringExtra(ImageReaderActivity.KEY_DATA)
            data?.let { d ->
                assertTrue(d.isEmpty() || d.isBlank())
            }?:run {
                assertNotNull(data)
            }
        }
    }

    /*
    * This test needs a real device to run.
    * Please face the rear camera to single face.
    * Do not face the rear camera towards multiple face or no face.
    */
    @Test
    fun testFaceResultPositive() {
        ImageReaderActivity.recognizeType = RecognizeType.FACE
        launchActivity<ImageReaderActivity>().use { it ->
            onView(withId(R.id.image_capture_button)).perform(click())

            val resultCode = it.result.resultCode
            assertEquals(ImageReaderActivity.RESULT_SUCCESS, resultCode)

            val resultData = it.result.resultData

            val type =  resultData.getStringExtra(ImageReaderActivity.KEY_RECOGNIZE_TYPE)
            type?.let { t->
                assertEquals(RecognizeType.FACE,RecognizeType.valueOf(t))
            }?: run {
                assertNotNull(type)
            }

            val numberOfFaces =  resultData.getIntExtra(ImageReaderActivity.KEY_NUMBER_OF_FACES,-1)
            assertTrue(numberOfFaces==1)

            val data = resultData.getStringExtra(ImageReaderActivity.KEY_DATA)
            data?.let { d ->
                assertTrue(d.isNotEmpty() && d.isNotBlank())
            }?:run {
                assertNotNull(data)
            }
        }
    }

    /*
    * This test needs a real device to run.
    * Do not face the rear camera to single face.
    * Face the rear camera towards multiple face or no face.
    */
    @Test
    fun testFaceResultNegative(){
        ImageReaderActivity.recognizeType = RecognizeType.FACE
        launchActivity<ImageReaderActivity>().use { it ->
            onView(withId(R.id.image_capture_button)).perform(click())

            val resultCode = it.result.resultCode
            assertEquals(ImageReaderActivity.RESULT_ERROR, resultCode)

            val resultData = it.result.resultData

            val type =  resultData.getStringExtra(ImageReaderActivity.KEY_RECOGNIZE_TYPE)
            type?.let { t->
                assertEquals(RecognizeType.FACE,RecognizeType.valueOf(t))
            }?: run {
                assertNotNull(type)
            }

            val numberOfFaces =  resultData.getIntExtra(ImageReaderActivity.KEY_NUMBER_OF_FACES,-1)
            assertTrue(numberOfFaces==0||numberOfFaces>1)

            val data = resultData.getStringExtra(ImageReaderActivity.KEY_DATA)
            data?.let { d ->
                assertTrue(d.isEmpty() || d.isBlank())
            }?:run {
                assertNotNull(data)
            }
        }
    }


}