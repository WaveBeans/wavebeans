package io.wavebeans.fs.core

import io.wavebeans.fs.local.LocalWbFileDriver
import java.net.URI

/**
 * File system abstract interface that allows seamlessly work with different storages.
 *
 * Use [registerDriver] to register the driver and assign it to specific scheme.
 * Use [instance] to get an instance of the driver by the scheme name.
 */
interface WbFileDriver {

    companion object {

        private val registry = hashMapOf<String, WbFileDriver>()

        init {
            registerDriver("file", LocalWbFileDriver)
        }

        /**
         * Registers the driver for the specified scheme.
         *
         * @param scheme the scheme the driver will be accessible under, case doesn't matter.
         * @param driver the driver implementation instance that register under the scheme.
         */
        fun registerDriver(scheme: String, driver: WbFileDriver) {
            registry.putIfAbsent(scheme.toLowerCase(), driver)
                    ?.let { throw IllegalStateException("Scheme $scheme is already registered: $it") }
        }

        /**
         * Unregisters the driver. Does nothing if the driver is not registered.
         *
         * @param scheme the driver scheme to unregister
         *
         * @return instance of removed driver if any
         */
        fun unregisterDriver(scheme: String): WbFileDriver? = registry.remove(scheme)

        /**
         * Finds the [WbFileDriver] for the specified scheme. The scheme should be registered via [registerDriver] call.
         *
         * @param scheme the scheme to search the driver for, case doesn't matter
         *
         * @return [WbFileDriver] to work with the specified scheme
         * @throws IllegalArgumentException if the driver can't be located
         */
        fun instance(scheme: String): WbFileDriver =
                registry[scheme.toLowerCase()]
                        ?: throw IllegalArgumentException("Scheme $scheme can be found among registered $registry")

        /**
         * Creates the file based on the scheme from URI [URI.scheme].
         * The scheme should be registered via [registerDriver] call.
         *
         * @throws IllegalArgumentException if the driver can't be located
         */
        fun createFile(uri: URI): WbFile =
                registry[uri.scheme.toLowerCase()]
                        ?.createWbFile(uri)
                        ?: throw IllegalArgumentException("Can't locate correct driver for URI $uri among registered $registry")

    }

    /**
     * Creates temporary file in the temporary folder with the format like "prefix.randomSymbols.suffix",
     * it makes a several attempt to create the file and makes sure the file doesn't exists at the moment of call.
     * Though that doesn't mean the file will not appear later under the same name.
     *
     * @param prefix the prefix part of the file preceding the random part
     * @param suffix the suffex part of the file tailing the random part
     * @param parent custom parent folder, if not specified the default one is being used (depends on driver implemenetation)
     *
     * @return file pointer instance. The file itself is not created or checked of existence.
     */
    fun createTemporaryWbFile(prefix: String, suffix: String, parent: WbFile? = null): WbFile

    /**
     * Creates the file pointer based on the URI, [URI.scheme] must correspond to the driver instance.
     *
     * @return file pointer instance. The file itself is not created or checked of existence.
     */
    fun createWbFile(uri: URI): WbFile

}