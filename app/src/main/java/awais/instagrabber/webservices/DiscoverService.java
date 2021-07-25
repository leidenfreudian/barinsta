package awais.instagrabber.webservices;

import androidx.annotation.NonNull;

import com.google.common.collect.ImmutableMap;

import java.util.Objects;

import awais.instagrabber.repositories.DiscoverRepository;
import awais.instagrabber.repositories.responses.discover.TopicalExploreFeedResponse;
import awais.instagrabber.utils.TextUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverService {

    private static final String TAG = "DiscoverService";

    private final DiscoverRepository repository;

    private static DiscoverService instance;

    private DiscoverService() {
        repository = RetrofitFactory.INSTANCE
                                    .getRetrofit()
                                    .create(DiscoverRepository.class);
    }

    public static DiscoverService getInstance() {
        if (instance == null) {
            instance = new DiscoverService();
        }
        return instance;
    }

    public void topicalExplore(final String maxId,
                               final ServiceCallback<TopicalExploreFeedResponse> callback) {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                .put("module", "explore_popular");
        if (!TextUtils.isEmpty(maxId)) {
            builder.put("max_id", maxId);
        }
        final Call<TopicalExploreFeedResponse> req = repository.topicalExplore(builder.build());
        req.enqueue(new Callback<TopicalExploreFeedResponse>() {
            @Override
            public void onResponse(@NonNull final Call<TopicalExploreFeedResponse> call,
                                   @NonNull final Response<TopicalExploreFeedResponse> response) {
                if (callback == null) return;
                final TopicalExploreFeedResponse feedResponse = response.body();
                if (feedResponse == null) {
                    callback.onSuccess(null);
                    return;
                }
                callback.onSuccess(feedResponse);
            }

            @Override
            public void onFailure(@NonNull final Call<TopicalExploreFeedResponse> call, @NonNull final Throwable t) {
                callback.onFailure(t);
            }
        });
    }
}
