package de.fraunhofer.iosb.app.edc;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.asset.AssetLoader;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

/**
 * Internal communication with EDC. Manages EDC assets and contracts.
 */
public class ResourceHandler {

    private final AssetLoader assetLoader;
    private final OkHttpClient okHttpClient;

    public ResourceHandler(AssetLoader assetLoader, OkHttpClient okHttpClient) {
        this.assetLoader = assetLoader;
        this.okHttpClient = okHttpClient;
    }

    /**
     * Register an asset at the EDC.

     * @param sourceUrl Data of the asset
     * @return asset ID of created asset
     */
    public String createAsset(String sourceUrl) {
        return createAsset(sourceUrl, null);
    }

    public String createAsset(String sourceUrl, String submodelIdentifier) {
        final var assetId = String.valueOf(sourceUrl.hashCode());
        final var dataAddress = HttpDataAddress.Builder.newInstance().baseUrl(sourceUrl).build();
        final var asset = Asset.Builder.newInstance().id(assetId.toString()).build();
        assetLoader.accept(asset, dataAddress);
        return assetId;
    }

    /**
     * Removes asset from assetIndex.

     * @param assetId asset id
     */
    public void deleteAsset(String assetId) {
        assetLoader.deleteById(assetId);
    }
}
