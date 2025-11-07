package de.fraunhofer.iosb.repository;

import java.nio.file.Path;
import java.util.Optional;

public abstract class AasRepositoryConfig<C> {
    public abstract C get();


    protected abstract static class Builder<B extends Builder<B, C>, C extends AasRepositoryConfig<?>> {
        protected Path model;

        public abstract B self();

        public B model(Path pathToModel) {
            this.model = pathToModel;
            return self();
        }

        public B model(String pathToModel) {
            this.model = Optional.ofNullable(pathToModel)
                    .map(Path::of)
                    .orElse(null);

            return self();
        }

        public abstract C build();
    }
}
