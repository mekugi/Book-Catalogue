/*
 * @copyright 2011 Philip Warner
 * @license GNU General Public License
 * 
 * This file is part of Book Catalogue.
 *
 * Book Catalogue is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Book Catalogue is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Book Catalogue.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.eleybourn.bookcatalogue;

import java.util.ArrayList;

import android.os.Bundle;

import com.eleybourn.bookcatalogue.Series.SeriesDetails;
import com.eleybourn.bookcatalogue.utils.Logger;
import com.eleybourn.bookcatalogue.utils.Utils;

abstract public class SearchThread extends ManagedTask {
	protected String mAuthor;
	protected String mTitle;
	protected String mIsbn;
	protected static boolean mFetchThumbnail;

	public enum DataSource {
		Amazon(0),
		Google(SearchManager.SEARCH_GOOGLE),
		OpenLibrary(0),
		LibraryThing(SearchManager.SEARCH_LIBRARY_THING),
		Goodreads(SearchManager.SEARCH_GOODREADS),
		BCDB(SearchManager.SEARCH_BC),
		Other(0),
		;
		private int mValue;

		private DataSource(int value) {
			mValue = value;
		}

		public int getValue() {
			return mValue;
		}
	}

	public static class BookSearchResults {
		public DataSource source;
		public Bundle data;
		public BookSearchResults(DataSource source, Bundle data) {
			this.source = source;
			this.data = data;
		}
	}

	// Accumulated book info.
	protected ArrayList<BookSearchResults> mResults = new ArrayList<>();

	/**
	 * Constructor. Will search according to passed parameters. If an ISBN
	 * is provided that will be used to the exclusion of all others.
	 * 
	 * @param manager		TaskManager
	 * @param author		Author to search for
	 * @param title			Title to search for
	 * @param isbn			ISBN to search for.
	 */
	public SearchThread(TaskManager manager, String author, String title, String isbn, boolean fetchThumbnail) {
		super(manager);
		mAuthor = author;
		mTitle = title;
		mIsbn = isbn;
		mFetchThumbnail = fetchThumbnail;

		//mBookData.putString(CatalogueDBAdapter.KEY_AUTHOR_FORMATTED, mAuthor);
		//mBookData.putString(CatalogueDBAdapter.KEY_TITLE, mTitle);
		//mBookData.putString(CatalogueDBAdapter.KEY_ISBN, mIsbn);
		//getMessageSwitch().addListener(getSenderId(), taskHandler, false);
	}

	public abstract DataSource getSearchId();

	@Override
	protected void onThreadFinish() {
		doProgress("Done",0);
	}

	/**
	 * Look in the data for a title, if present try to get a series name from it.
	 * In any case, clear the title (and save if none saved already) so that the 
	 * next lookup will overwrite with a possibly new title.
	 */
	protected void checkForSeriesName() {
		for(BookSearchResults result: mResults) {
			Bundle bookData = result.data;
			try {
				if (bookData.containsKey(CatalogueDBAdapter.KEY_TITLE)) {
					String thisTitle = bookData.getString(CatalogueDBAdapter.KEY_TITLE);
					SeriesDetails details = Series.findSeries(thisTitle);
					if (details != null && details.name.length() > 0) {
						ArrayList<Series> sl;
						if (bookData.containsKey(CatalogueDBAdapter.KEY_SERIES_DETAILS)) {
							sl = Utils.getSeriesUtils().decodeList(bookData.getString(CatalogueDBAdapter.KEY_SERIES_DETAILS), '|', false);
						} else {
							sl = new ArrayList<Series>();
						}
						sl.add(new Series(details.name, details.position));
						bookData.putString(CatalogueDBAdapter.KEY_SERIES_DETAILS, Utils.getSeriesUtils().encodeList(sl, '|'));
						bookData.putString(CatalogueDBAdapter.KEY_TITLE, thisTitle.substring(0, details.startChar-1));
					}
				}
			} catch (Exception e) {
				Logger.logError(e);
			}
		}
	}
	
	protected void showException(int id, Exception e) {
		String s;
		try {s = e.getMessage(); } catch (Exception e2) {s = "Unknown Exception";};
		String msg = String.format(getString(R.string.search_exception), getString(id), s);
		doToast(msg);		
	}
	
	/**
	 * Accessor, so when thread has finished, data can be retrieved.
	 */
	public ArrayList<BookSearchResults> getBookData() {
		return mResults;
	}
}
