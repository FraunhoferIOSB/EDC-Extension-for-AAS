package de.fraunhofer.iosb.dataplane.aas.spi;

import de.fraunhofer.iosb.model.aas.service.Service;
import de.fraunhofer.iosb.util.Encoder;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AasDataAddressTest {

    @Test
    void test_build_accessUrlBuiltCorrectly() throws MalformedURLException {
        var addressBuilder = AasDataAddress.Builder.newInstance();

        addressBuilder.aasProvider(new Service(new URL("http://localhost:8080")));
        addressBuilder.path("/path/to/resource");

        var address = addressBuilder.build();

        assertEquals(address.getAccessUrl().getContent().toString(), "http://localhost:8080");

        assertEquals(address.getPath(), "/path/to/resource");
    }

    @Test
    void test_build_accessUrlBuiltCorrectlyWithProvider() throws MalformedURLException {
        var addressBuilder = AasDataAddress.Builder.newInstance();

        addressBuilder.aasProvider(new Service(new URL("http://aas-provider:8081")));

        addressBuilder.path("/path/to/resource");

        var address = addressBuilder.build();
        assertEquals("http://aas-provider:8081", address.getAccessUrl().getContent().toString());

        assertEquals(address.getPath(), "/path/to/resource");
    }

    @Test
    void test_build_returnCorrectReferenceChainAsPathNested() {
        String smIdShort = "sm";

        var keys = new ArrayList<Key>();
        String path = "/submodel-elements/";
        for (int i = 0; i < 100; i++) {
            keys.add(getKey(KeyTypes.SUBMODEL_ELEMENT_COLLECTION, "smc%s".formatted(i)));
            path = path.concat("smc%s.".formatted(i));

        }
        keys.add(getKey(KeyTypes.SUBMODEL_ELEMENT, "sme"));
        path = path.concat("sme");

        var referenceChain = new DefaultReference.Builder()
                .keys(keys)
                .build();

        assertEquals(path.formatted(Encoder.encodeBase64(smIdShort)),
                AasDataAddress.Builder.newInstance().referenceChain(referenceChain).build().getPath());
    }

    @Test
    void test_build_returnCorrectReferenceChainAsPathSubmodel() {
        String smIdShort = "sm";
        var referenceChain = new DefaultReference.Builder()
                .keys(List.of(
                        getKey(KeyTypes.SUBMODEL, smIdShort)
                ))
                .build();

        assertEquals("submodels/%s".formatted(Encoder.encodeBase64(smIdShort)),
                AasDataAddress.Builder.newInstance().referenceChain(referenceChain).build().getPath());
    }

    @Test
    void test_build_returnCorrectReferenceChainAsPathShell() {
        String shellIdShort = "shell";
        var referenceChain = new DefaultReference.Builder()
                .keys(List.of(getKey(KeyTypes.ASSET_ADMINISTRATION_SHELL, shellIdShort)))
                .build();

        assertEquals("shells/%s".formatted(Encoder.encodeBase64(shellIdShort)),
                AasDataAddress.Builder.newInstance().referenceChain(referenceChain).build().getPath());
    }

    @Test
    void test_build_returnCorrectReferenceChainAsPathConceptDescription() {
        String cdIdShort = "cd";
        var referenceChain = new DefaultReference.Builder()
                .keys(List.of(getKey(KeyTypes.CONCEPT_DESCRIPTION, cdIdShort)))
                .build();

        assertEquals("concept-descriptions/%s".formatted(Encoder.encodeBase64(cdIdShort)),
                AasDataAddress.Builder.newInstance().referenceChain(referenceChain).build().getPath());
    }

    private Key getKey(KeyTypes keyType, String idShort) {
        return new DefaultKey.Builder()
                .type(keyType)
                .value(idShort)
                .build();
    }
}