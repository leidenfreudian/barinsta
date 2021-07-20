package awais.instagrabber.asyncs;

import java.util.List;

import awais.instagrabber.customviews.helpers.PostFetcher;
import awais.instagrabber.interfaces.FetchListener;
import awais.instagrabber.models.enums.PostItemType;
import awais.instagrabber.repositories.responses.Media;
import awais.instagrabber.repositories.responses.PostsFetchResponse;
import awais.instagrabber.utils.CoroutineUtilsKt;
import awais.instagrabber.webservices.GraphQLRepository;
import awais.instagrabber.webservices.ProfileRepository;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;

public class SavedPostFetchService implements PostFetcher.PostFetchService {
    private final ProfileRepository profileRepository;
    private final GraphQLRepository graphQLRepository;
    private final long profileId;
    private final PostItemType type;
    private final boolean isLoggedIn;

    private String nextMaxId;
    private final String collectionId;
    private boolean moreAvailable;

    public SavedPostFetchService(final long profileId, final PostItemType type, final boolean isLoggedIn, final String collectionId) {
        this.profileId = profileId;
        this.type = type;
        this.isLoggedIn = isLoggedIn;
        this.collectionId = collectionId;
        graphQLRepository = isLoggedIn ? null : GraphQLRepository.Companion.getInstance();
        profileRepository = isLoggedIn ? ProfileRepository.Companion.getInstance() : null;
    }

    @Override
    public void fetch(final FetchListener<List<Media>> fetchListener) {
        final Continuation<PostsFetchResponse> callback = CoroutineUtilsKt.getContinuation((result, t) -> {
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
        switch (type) {
            case LIKED:
                profileRepository.fetchLiked(nextMaxId, callback);
                break;
            case TAGGED:
                if (isLoggedIn) profileRepository.fetchTagged(profileId, nextMaxId, callback);
                else graphQLRepository.fetchTaggedPosts(
                        profileId,
                        30,
                        nextMaxId,
                        callback
                );
                break;
            case COLLECTION:
            case SAVED:
                profileRepository.fetchSaved(nextMaxId, collectionId, callback);
                break;
        }
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
