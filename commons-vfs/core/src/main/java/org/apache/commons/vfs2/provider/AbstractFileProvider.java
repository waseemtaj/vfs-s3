/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.vfs2.provider;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.local.GenericFileNameParser;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A partial {@link FileProvider} implementation.  Takes care of managing the
 * file systems created by the provider.
 */
public abstract class AbstractFileProvider
    extends AbstractVfsContainer
    implements FileProvider
{
    /**
     * The cached file systems.  This is a mapping from root URI to
     * FileSystem object.
     */
    private final ConcurrentMap<FileSystemKey, FileSystem> fileSystems = new ConcurrentHashMap<>();

    private FileNameParser parser;

    public AbstractFileProvider()
    {
        parser = GenericFileNameParser.getInstance();
    }

    protected FileNameParser getFileNameParser()
    {
        return parser;
    }

    protected void setFileNameParser(final FileNameParser parser)
    {
        this.parser = parser;
    }

    /**
     * Closes the file systems created by this provider.
     */
    @Override
    public void close()
    {
        fileSystems.clear();

        super.close();
    }

    /**
     * Creates a layered file system.  This method throws a 'not supported' exception.
     * @param scheme The protocol to use to access the file.
     * @param file a FileObject.
     * @param properties Options to the file system.
     * @return A FileObject associated with the new FileSystem.
     * @throws FileSystemException if an error occurs.
     */
    @Override
    public FileObject createFileSystem(final String scheme, final FileObject file, final FileSystemOptions properties)
        throws FileSystemException
    {
        // Can't create a layered file system
        throw new FileSystemException("vfs.provider/not-layered-fs.error", scheme);
    }

    /**
     * Adds a file system to those cached by this provider.
     * <p>
     * The file system may implement {@link VfsComponent}, in which case it is initialised.
     * @param key The root file of the file system, part of the cache key.
     * @param fs the file system to add.
     * @throws FileSystemException if any error occurs.
     */
    protected void addFileSystem(final Comparable<?> key, FileSystemOptions fileSystemOptions, final FileSystem fs)
        throws FileSystemException
    {
        // Add to the cache
        addComponent(fs);

        final FileSystemKey treeKey = new FileSystemKey(key, fileSystemOptions);
        ((AbstractFileSystem) fs).setCacheKey(treeKey);

        fileSystems.put(treeKey, fs);
    }

    /**
     * Locates a cached file system.
     *
     * @param key The root file of the file system, part of the cache key.
     * @param fileSystemProps file system options the file system instance must have.
     * @return The file system instance, or null if it is not cached.
     */
    protected FileSystem findFileSystem(final Comparable<?> key, final FileSystemOptions fileSystemProps)
    {
        final FileSystemKey treeKey = new FileSystemKey(key, fileSystemProps);

        return fileSystems.get(treeKey);
    }

    /**
     * Returns the FileSystemConfigBuidler.
     * @return the FileSystemConfigBuilder.
     */
    @Override
    public FileSystemConfigBuilder getConfigBuilder()
    {
        return null;
    }

    /**
     * Free unused resources.
     */
    public void freeUnusedResources()
    {
        for (final Object element : fileSystems.values().toArray())
        {
            final AbstractFileSystem fs = (AbstractFileSystem) element;
            if (fs.isReleaseable())
            {
                fs.closeCommunicationLink();
            }
        }
    }

    /**
     * Close the FileSystem.
     * @param filesystem The FileSystem to close.
     */
    public void closeFileSystem(final FileSystem filesystem)
    {
        final AbstractFileSystem fs = (AbstractFileSystem) filesystem;

        if (fs.getCacheKey() != null)
        {
            fileSystems.remove(fs.getCacheKey());
        }

        removeComponent(fs);
        fs.close();
    }

    /**
     * Parses an absolute URI.
     *
     * @param base The base file - if null the {@code uri} needs to be absolute
     * @param uri The URI to parse.
     * @return The FileName.
     * @throws FileSystemException if an error occurs.
     */
    @Override
    public FileName parseUri(final FileName base, final String uri) throws FileSystemException
    {
        if (getFileNameParser() != null)
        {
            return getFileNameParser().parseUri(getContext(), base, uri);
        }

        throw new FileSystemException("vfs.provider/filename-parser-missing.error");
    }
}
