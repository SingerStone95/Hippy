package com.tencent.mtt.hippy.views.viewpager.transform;

import android.support.v4.view.ViewPager;
import android.view.View;

public class VerticalPageTransformer implements ViewPager.PageTransformer {

  @Override
  public void transformPage(View view, float position) {
    if (position >= -1 && position <= 1) {
      view.setTranslationX(view.getWidth() * -position);
      float yPosition = position * view.getHeight();
      view.setTranslationY(yPosition);
    }
  }

}
