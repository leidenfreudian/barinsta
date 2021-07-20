package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.repositories.responses.User;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.ProfileRepository;
import awais.instagrabber.webservices.ServiceCallback;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

public class ProfilePostFetchService implements PostFetcher.PostFetchService {
    private static final String TAG = "ProfilePostFetchService";
    private final ProfileRepository profileRepository;
    private final GraphQLRepository graphQLRepository;
    private final User profileModel;
    private final boolean isLoggedIn;
    private String nextMaxId;
    private boolean moreAvailable;

    public ProfilePostFetchService(final User profileModel, final boolean isLoggedIn) {
        this.profileModel = profileModel;
        this.isLoggedIn = isLoggedIn;
        graphQLRepository = isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
        profileRepository = isLoggedIn ? ProfileRepository.Companion.getInstance() : null;
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
        if (isLoggedIn) profileRepository.fetchPosts(profileModel.getPk(), nextMaxId, cb);
        else graphQLRepository.fetchProfilePosts(
                profileModel.getPk(),
                30,
                nextMaxId,
                profileModel,
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
