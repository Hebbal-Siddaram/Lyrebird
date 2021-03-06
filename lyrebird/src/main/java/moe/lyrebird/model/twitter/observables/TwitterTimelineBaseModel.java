/*
 *     Lyrebird, a free open-source cross-platform twitter client.
 *     Copyright (C) 2017-2018, Tristan Deloche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package moe.lyrebird.model.twitter.observables;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import moe.lyrebird.model.sessions.SessionManager;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * This is the base class for reverse-chronologically sorted tweet lists (aka Timelines) backend model.
 */
public abstract class TwitterTimelineBaseModel {

    protected final SessionManager sessionManager;

    private final AtomicBoolean isFirstCall = new AtomicBoolean(true);

    private final ObservableList<Status> loadedTweets = FXCollections.observableList(new LinkedList<>());

    public TwitterTimelineBaseModel(final SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * @return The currently loaded tweets.
     */
    public ObservableList<Status> loadedTweets() {
        return FXCollections.unmodifiableObservableList(loadedTweets);
    }

    /**
     * Asynchronously requests loading of tweets prior to the given status.
     *
     * @param loadUntilThisStatus the status whose prior tweets are requested
     */
    public void loadMoreTweets(final long loadUntilThisStatus) {
        CompletableFuture.runAsync(() -> {
            getLocalLogger().debug("Requesting more tweets.");
            final Paging requestPaging = new Paging();
            requestPaging.setMaxId(loadUntilThisStatus);

            sessionManager.getCurrentTwitter()
                          .mapTry(twitter -> backfillLoad(twitter, requestPaging))
                          .onSuccess(this::addTweets);
            getLocalLogger().debug("Finished loading more tweets.");
        });
    }

    /**
     * Asynchronously loads the last tweets available
     */
    public void refresh() {
        CompletableFuture.runAsync(() -> {
            if (sessionManager.getCurrentTwitter().getOrElse((Twitter) null) == null) {
                return;
            }
            getLocalLogger().debug("Requesting last tweets in timeline.");
            sessionManager.getCurrentTwitter()
                          .mapTry(this::initialLoad)
                          .onSuccess(this::addTweets)
                          .onFailure(err -> getLocalLogger().error("Could not refresh!", err))
                          .andThen(() -> isFirstCall.set(false));
        });
    }

    /**
     * Add a given list of tweets to the currently loaded ones.
     *
     * @param receivedTweets The tweets to add.
     */
    private void addTweets(final List<Status> receivedTweets) {
        final int newTweets = receivedTweets.stream().map(this::addTweet).mapToInt(val -> val ? 1 : 0).sum();
        getLocalLogger().debug("Loaded {} new tweets.", newTweets);
    }

    /**
     * Adds a single tweet to the list of currently loaded ones.
     *
     * @param newTweet The tweet to add.
     */
    private boolean addTweet(final Status newTweet) {
        if (!this.loadedTweets.contains(newTweet)) {
            this.loadedTweets.add(newTweet);
            this.loadedTweets.sort(Comparator.comparingLong(Status::getId).reversed());
            if (!isFirstCall.get()) {
                onNewElementStreamed(newTweet);
            }
            return true;
        }
        return false;
    }

    protected void onNewElementStreamed(final Status newElement) {
        // do nothing by default
    }

    /**
     * Remove all loaded tweets.
     */
    void clearLoadedTweets() {
        loadedTweets.clear();
    }

    /**
     * Performs the initial load of tweets (i.e. {@link #refresh()}).
     *
     * @param twitter The twitter instance to use
     *
     * @return The list of tweets received from Twitter
     * @throws TwitterException if there was an issue loading tweets
     */
    protected abstract List<Status> initialLoad(final Twitter twitter)
    throws TwitterException;

    /**
     * Performs a request for loading more tweets (i.e. {@link #loadMoreTweets(long)}).
     *
     * @param twitter The twitter instance to use
     * @param paging  Parameters for the request (containing the tweet whose prior tweets are requested for example)
     *
     * @return The list of tweets received from Twitter
     * @throws TwitterException if there was an issue loading tweets
     */
    protected abstract List<Status> backfillLoad(final Twitter twitter, final Paging paging)
    throws TwitterException;

    /**
     * @return The subclass' logger that will be used for logging requests and potential errors
     */
    protected abstract Logger getLocalLogger();

}
