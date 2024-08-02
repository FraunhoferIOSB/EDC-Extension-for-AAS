package de.fraunhofer.iosb.app.sync;

import org.junit.jupiter.api.Test;

class SynchronizationManagerTest {

    /**
     * Test for failure:
     * - no synchronizers registered
     * - test synchronize
     * - test remove
     * - test created
     * - wrong synchronizer for job
     * - test synchronize
     * - test remove
     * - test created
     * Verify correct behaviour:
     * - Correct synchronizer registered
     * - test synchronize command gets through to synchronizer
     * - test created command gets through to synchronizer
     * - test removed command gets through to synchronizer
     */
    @Test
    void run() {
    }

    @Test
    void created() {
    }

    @Test
    void removed() {
    }
}