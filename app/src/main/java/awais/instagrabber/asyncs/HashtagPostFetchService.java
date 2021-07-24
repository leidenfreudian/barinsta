package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Hashtag;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.HashtagRepository;
import awais.instagrabber.webservices.ServiceCallback;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

public class HashtagPostFetchService implements PostFetcher.PostFetchService {
    private final HashtagRepository hashtagRepository;
    private final GraphQLRepository graphQLRepository;
    private final Hashtag hashtagModel;
    private String nextMaxId;
    private boolean moreAvailable;
    private final boolean isLoggedIn;

    public HashtagPostFetchService(final Hashtag hashtagModel, final boolean isLoggedIn) {
        this.hashtagModel = hashtagModel;
        this.isLoggedIn = isLoggedIn;
        hashtagRepository = isLoggedIn ? HashtagRepository.Companion.getInstance() : null;
        graphQLRepository = isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
    }

    @Override
    public void fetch(final FetchListener<List<Media>> fetchListener) {
        final Continuation<PostsFetchResponse> cb = CoroutineUtilsKt.getContinuation((result, t) -> {
            if (t != null) {
                if (fetchListener != null) {
                    fetchListener.onFailure(t);
                }
                return;
            }
            if (result == null) return;
            nextMaxId = result.getNextCursor();
            moreAvailable = result.getHasNextPage();
            if (fetchListener != null) {
                fetchListener.onResult(result.getFeedModels());
            }
        }, Dispatchers.getIO());
        if (isLoggedIn) hashtagRepository.fetchPosts(hashtagModel.getName().toLowerCase(), nextMaxId, cb);
        else graphQLRepository.fetchHashtagPosts(
                hashtagModel.getName().toLowerCase(),
                nextMaxId,
                cb
        );
    }

    @Override
    public void reset() {
        nextMaxId = null;
    }

    @Override
    public boolean hasNextPage() {
        return moreAvailable;
    }
}
