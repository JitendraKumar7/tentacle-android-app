package com.sunoray.tentacle.layout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.sunoray.tentacle.R;

public class TableViewSwipeRefreshLayout extends SwipeRefreshLayout {
	private ScrollView view;
	
	public TableViewSwipeRefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setView(ScrollView view) {
		this.view = view;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		view = (ScrollView) findViewById(R.id.main_screen_scroll_view);
	}

	@SuppressLint("NewApi")
	@Override
	public boolean canChildScrollUp() {
		// refresh only can work in android 4+ (ICS)
		if(android.os.Build.VERSION.SDK_INT< android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			return true;
		else
			return view.canScrollVertically(-1);
	}
	
	@Override
	public void setColorSchemeColors(int... colors) {		
		super.setColorSchemeColors(new int[]{Color.rgb(0, 101, 67)});
	}
}
