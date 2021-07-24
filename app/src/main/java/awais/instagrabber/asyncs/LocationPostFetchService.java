package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Location;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.LocationRepository;
import awais.instagrabber.webservices.ServiceCallback;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

public class LocationPostFetchService implements PostFetcher.PostFetchService {
    private final LocationRepository locationRepository;
    private final GraphQLRepository graphQLRepository;
    private final Location locationModel;
    private String nextMaxId;
    private boolean moreAvailable;
    private final boolean isLoggedIn;

    public LocationPostFetchService(final Location locationModel, final boolean isLoggedIn) {
        this.locationModel = locationModel;
        this.isLoggedIn = isLoggedIn;
        locationRepository = isLoggedIn ? LocationRepository.Companion.getInstance() : null;
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
        if (isLoggedIn) locationRepository.fetchPosts(locationModel.getPk(), nextMaxId, cb);
        else graphQLRepository.fetchLocationPosts(
                locationModel.getPk(),
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
