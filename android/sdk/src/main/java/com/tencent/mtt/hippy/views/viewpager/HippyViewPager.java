/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.views.viewpager;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import com.tencent.mtt.hippy.HippyInstanceContext;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.uimanager.HippyViewBase;
import com.tencent.mtt.hippy.uimanager.NativeGestureDispatcher;
import com.tencent.mtt.hippy.utils.LogUtils;
import com.tencent.mtt.hippy.views.viewpager.transform.VerticalPageTransformer;
import com.tencent.mtt.supportui.views.ScrollChecker;
import java.lang.reflect.Field;


public class HippyViewPager extends ViewPager implements HippyViewBase
{
	private static final String			TAG					= "HippyViewPager";

	private final Runnable				mMeasureAndLayout	= new Runnable()
															{
																@Override
																public void run()
																{
																	measure(View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY),
																			View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.EXACTLY));
																	layout(getLeft(), getTop(), getRight(), getBottom());
																}
															};


	private NativeGestureDispatcher mGestureDispatcher;
	private boolean						mScrollEnabled		= true;
	private boolean						mFirstUpdateChild	= true;
	private boolean 					mReNotifyOnAttach = false;
	private ViewPagerPageChangeListener mPageListener;
	private String								mOverflow;
	private Handler						mHandler			= new Handler(Looper.getMainLooper());
  private Promise mCallBackPromise;
  private PageSelectedListener mSelectedListener;
  private boolean mReLayoutOnAttachToWindow = true;
  private boolean mCallPageChangedOnFirstLayout = false;
  private boolean mIsCallPageChangedOnFirstLayout = false;
  private int mOldState = SCROLL_STATE_IDLE;
  private boolean mIsVertical = false;



  public interface PageSelectedListener {

    void onPageSelected(boolean programmed, int newIndex);
  }

  public void setPageSelectedListener(PageSelectedListener listener) {
    mSelectedListener = listener;
  }

  private void init(Context context) {
    Log.d("yogachen", "current page=" + this.getClass().getName());
    setCallPageChangedOnFirstLayout(true);
    setEnableReLayoutOnAttachToWindow(false);
    mPageListener = new ViewPagerPageChangeListener(this);
    addOnPageChangeListener(mPageListener);
    setAdapter(createAdapter(context));
    initViewPager();
  }

  public int getCurrentPage() {
    return getCurrentItem();
  }



  protected void initViewPager() {
    //内部持有一个
    addOnPageChangeListener(new OnPageChangeListener() {
      @Override
      public void onPageScrolled(int i, float v, int i1) {
      }

      @Override
      public void onPageSelected(int i) {
      }

      @Override
      public void onPageScrollStateChanged(int i) {
        notifyScrollStateChanged(mOldState, i);
        mOldState = i;
      }
    });
    handleCallPageChangeFirstLayout();
    if (mIsVertical) {
      setPageTransformer(true, new VerticalPageTransformer());
      // The easiest way to get rid of the overscroll drawing that happens on the left and right
      setOverScrollMode(OVER_SCROLL_NEVER);
    }
  }

  private void handleCallPageChangeFirstLayout() {
    getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      @RequiresApi(api = VERSION_CODES.JELLY_BEAN)
      @Override
      public void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (mPageListener != null && mCallPageChangedOnFirstLayout) {
          mPageListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_IDLE);
        }
      }
    });
  }
  public int getPageCount() {
    return getAdapter() == null ? 0 : getAdapter().getCount();
  }
  public Object getCurrentItemView() {
    if (getAdapter() != null) {
      return getAdapter().getCurrentItemObj();
    }
    return null;
  }
  public HippyViewPager(Context context, boolean isVertical)
  {
    super(context);
    mIsVertical = isVertical;
    init(context);
  }

	public HippyViewPager(Context context)
	{
		super(context);
    init(context);
	}

	public void setCallBackPromise(Promise promise) {
    mCallBackPromise = promise;
  }

  public Promise getCallBackPromise() {
    return mCallBackPromise;
  }

  protected HippyViewPagerAdapter createAdapter(Context context)
	{
		return new HippyViewPagerAdapter((HippyInstanceContext) context, this);
	}

  public void setInitialPageIndex(final int index) {
    Log.i("yogachen",
      HippyViewPager.this.getClass().getName() + " " + "setInitialPageIndex=" + index);
    setCurrentItem(index);
    setDefaultItem(index);
    //对齐老的ViewPager，补一个回调
    //callPageSelected(index);
  }

  private void callPageSelected(int index) {
    if (!mIsCallPageChangedOnFirstLayout) {
      mIsCallPageChangedOnFirstLayout = true;
      if (mPageListener != null) {
        mPageListener.onPageSelected(index);
      }
    }
  }

  public void setChildCountAndUpdate(final int childCount)
	{
		LogUtils.d(TAG, "doUpdateInternal: " + hashCode() + ", childCount=" + childCount);
		if (mFirstUpdateChild)
		{
			setFirstLayoutParameter(true);
			mFirstUpdateChild = false;
		}
		getAdapter().setChildSize(childCount);
		//getWindowToken() == null执行这个操作，onAttachToWindow就不需要了。
		getAdapter().notifyDataSetChanged();
		triggerRequestLayout();
	}

	protected void addViewToAdapter(HippyViewPagerItem view, int postion)
	{
		HippyViewPagerAdapter adapter = getAdapter();
		if (adapter != null)
		{
			adapter.addView(view, postion);
		}
	}

	protected int getAdapterViewSize()
	{
		HippyViewPagerAdapter adapter = getAdapter();
		if (adapter != null)
		{
			return adapter.getItemViewSize();
		}
		return 0;
	}

	protected void removeViewFromAdapter(HippyViewPagerItem view)
	{
		HippyViewPagerAdapter adapter = getAdapter();
		if (adapter != null)
		{
			adapter.removeView(view);
		}
	}

	public View getViewFromAdapter(int currentItem)
	{
		HippyViewPagerAdapter adapter = getAdapter();
		if (adapter != null)
		{
			return adapter.getViewAt(currentItem);
		}
		return null;
	}

	@Override
	public HippyViewPagerAdapter getAdapter()
	{
		return (HippyViewPagerAdapter) super.getAdapter();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev)
	{
		if (!mScrollEnabled)
		{
			return false;
		}
		if (mIsVertical) {
      boolean intercepted = super.onInterceptTouchEvent(swapXY(ev));
      swapXY(ev); // return touch coordinates to original reference frame for any child views
      return intercepted;
    }
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		if (!mScrollEnabled)
		{
			return false;
		}
    if (mIsVertical) {
      return super.onTouchEvent(swapXY(ev));
    }
		return super.onTouchEvent(ev);
	}

  @Override
  public void setCurrentItem(int item, boolean smoothScroll) {
    super.setCurrentItem(item, smoothScroll);
    Log.i("yogachen",
      HippyViewPager.this.getClass().getName()+" setCurrentItem item="+item+" smoothScroll="+smoothScroll);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (!mReLayoutOnAttachToWindow) {
      setFirstLayout(false);
    }
/*
		LogUtils.d(TAG, "onAttachedToWindow: " + hashCode() + ", childCount=" + getChildCount() + ", repopulate=" + mNeedRepopulate
				+ ", renotifyOnAttach=" + mReNotifyOnAttach);
*/

		/*
		 * hippy 在setChildCountAndUpdate打开，执行了
		 * if (mReNotifyOnAttach)
		 * {
		 * getAdapter().notifyDataSetChanged();
		 * mReNotifyOnAttach = false;
		 * }
		 */
		// 9.6在supportui已经把windowToken的检查过滤去掉了，所以这里应该关掉
		/*
		 * if (mNeedRepopulate) //这个是是在supportUI工程里面poplate的时候设置的。再没有上树的情况下
		 * {
		 * mNeedRepopulate = false;
		 * triggerRequestLayout();
		 * postInvalidate();
		 * }
		 */
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
	/*	LogUtils.d(TAG, "onDetachedFromWindow: " + hashCode() + ", childCount=" + getChildCount() + ", repopulate=" + mNeedRepopulate
				+ ", renotifyOnAttach=" + mReNotifyOnAttach);*/
	}

	public void switchToPage(int item, boolean animated)
	{
		/*LogUtils.d(TAG, "switchToPage: " + hashCode() + ", item=" + item + ", animated=" + animated);
		if (getAdapter().getCount() == 0) // viewpager的children没有初始化好的时候，直接设置mInitialPageIndex
		{
			//			mInitialPageIndex = item;
			//			getAdapter().setInitPageIndex(item);
		}
		else
		{
			if (getCurrentItem() != item) // 如果和当前位置一样，就不进行switch
			{
				if (isSettling())
				{
					// 如果仍然在滑动中，重置一下状态
					setScrollingCacheEnabled(false);
					mScroller.abortAnimation();
					int oldX = getScrollX();
					int oldY = getScrollY();
					int x = mScroller.getCurrX();
					int y = mScroller.getCurrY();
					if (oldX != x || oldY != y)
					{
						scrollTo(x, y);
					}
					setScrollState(SCROLL_STATE_IDLE);
				}
				setCurrentItem(item, animated);
			}
			else if (!isFirstLayout())
			{
				mPageListener.onPageSelected(item);
			}
		}*/
    if (getAdapter() == null
      || getAdapter().getCount() == 0) // viewpager的children没有初始化好的时候，直接设置mInitialPageIndex
    {
      setInitialPageIndex(item);
    } else {
      setCurrentItem(item, animated);
      if (!isSetting()) {
        mPageListener.onPageSelected(item);
      }
    }
	}

  private boolean isSetting() {
    return mOldState == SCROLL_STATE_SETTLING;
  }

	public void setScrollEnabled(boolean scrollEnabled)
	{
		mScrollEnabled = scrollEnabled;
	}

	@Override
	public NativeGestureDispatcher getGestureDispatcher()
	{
		return mGestureDispatcher;
	}

	@Override
	public void setGestureDispatcher(NativeGestureDispatcher nativeGestureDispatcher)
	{
		mGestureDispatcher = nativeGestureDispatcher;
	}

	public void triggerRequestLayout()
	{
		mHandler.post(mMeasureAndLayout);
	}
	public void setOverflow(String overflow)
	{
		mOverflow = overflow;
		//robinsli Android 支持 overflow: visible，超出容器之外的属性节点也可以正常显示
		if(!TextUtils.isEmpty(mOverflow))
		{
			switch (mOverflow)
			{
				case "visible":
					setClipChildren(false); //可以超出父亲区域
					break;
				case "hidden":
				{
					setClipChildren(true); //默认值是false
					break;
				}
			}
		}
		invalidate();
	}
  public void onOverScrollSuccess() {

  }

  protected void notifyScrollStateChanged(int oldState, int newState) {

  }

  private void setEnableReLayoutOnAttachToWindow(boolean enable) {
    mReLayoutOnAttachToWindow = enable;
  }

  private void setCallPageChangedOnFirstLayout(boolean enable) {
    mCallPageChangedOnFirstLayout = enable;
  }

  private void setFirstLayoutParameter(boolean isFirstLayout) {
    setFirstLayout(isFirstLayout);
  }


  public void onTabPressed(int id) {
    if (mSelectedListener != null) {
      mSelectedListener.onPageSelected(true, id);
    }
  }




  public boolean onOverScroll(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
    int scrollRangeY, int maxOverScrollX,
    int maxOverScrollY, boolean isTouchEvent) {
    if (((scrollX == 0 && deltaX < 0) || (scrollX == scrollRangeX && deltaX > 0))) {
      onOverScrollSuccess();
    }
    return true;

  }

  @Override
  protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
    return ScrollChecker.canScroll(v, checkV, mIsVertical, dx, x, y);
  }

  private MotionEvent swapXY(MotionEvent ev) {
    float width = getWidth();
    float height = getHeight();

    float newX = (ev.getY() / height) * width;
    float newY = (ev.getX() / width) * height;

    ev.setLocation(newX, newY);

    return ev;
  }


  /**
   * hook 方法，不建议调用，这里只是为了兼容,目的是为了触发一次firstLayout恢复状态
   * @param isFirstLayout
   */
  private void setFirstLayout(boolean isFirstLayout) {
    try {
      Field field = ViewPager.class.getDeclaredField("mFirstLayout");
      field.setAccessible(true);
      field.set(this, isFirstLayout);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

 /**
   * 也是Hack方法，设置初始化index
   * @param position
   */
  private void setDefaultItem(int position) {
    try {
      Field field = ViewPager.class.getDeclaredField("mCurItem");
      field.setAccessible(true);
      field.setInt(this, position);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
