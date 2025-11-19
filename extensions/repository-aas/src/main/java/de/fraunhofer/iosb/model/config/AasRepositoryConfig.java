package de.fraunhofer.iosb.model.config;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;


public abstract class AasRepositoryConfig<C> {

    protected final List<PolicyBinding> policyBindings;


    protected AasRepositoryConfig(List<PolicyBinding> policyBindings) {this.policyBindings = policyBindings;}


    public abstract C get();


    public List<PolicyBinding> getPolicyBindings() {
        return policyBindings;
    }


    protected abstract static class Builder<B extends Builder<B, C>, C extends AasRepositoryConfig<?>> {
        protected Path model;
        protected List<PolicyBinding> policyBindings;


        public abstract B self();


        public B model(Path pathToModel) {
            this.model = pathToModel;
            return self();
        }


        public B policyBindings(List<PolicyBinding> policyBindings) {
            this.policyBindings = policyBindings;
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
