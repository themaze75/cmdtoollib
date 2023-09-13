/*
 * Copyright 2023 Eric Maziade
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.maziade.cmdtool.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.sun.jna.*;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

/**
 * When invoked, will use JNI to bind to Windows to get file version information for executable and DLL files.
 */
@Component
public class FileVersionInfoUtility
{
	private Version instance;

	/**
	 * We want to bind JNI only if we ask for it (non-windows environments will not ask)
	 * @return
	 */
	Version getInstance()
	{
		if (instance == null)
			instance = Native.load("Version", Version.class, W32APIOptions.UNICODE_OPTIONS);
		
		return instance;
	}

	/**
	 * @param fileName File name for which to get version (exe or dll)
	 * @return version number
	 */
	public Optional<String> getVersion(String fileName)
	{
		if (!Files.exists(Path.of(fileName)))
			return Optional.empty();

		final Version v = getInstance();
		final int verSize = v.GetFileVersionInfoSizeW(fileName, 0);

		final byte[] buf = new byte[verSize];
		try (final Memory lpData = new Memory(buf.length))
		{
	
			final PointerByReference lplpBuffer = new PointerByReference();
			final IntByReference dataSize = new IntByReference();
			boolean res = v.GetFileVersionInfoW(fileName, 0, verSize, lpData);

			if (!res)
				return Optional.empty();
	
			res = v.VerQueryValueW(lpData, "\\", lplpBuffer, dataSize);

			if (!res)
				return Optional.empty();

			VS_FIXEDFILEINFO lplpBufStructure = new VS_FIXEDFILEINFO(lplpBuffer.getValue());
			lplpBufStructure.read();
	
			final StringBuilder out = new StringBuilder();
	
			out.append(lplpBufStructure.dwFileVersionMS >> 16);
			out.append('.');
			out.append(lplpBufStructure.dwFileVersionMS & 0xffff);
			out.append('.');
			out.append(lplpBufStructure.dwFileVersionLS >> 16);
			out.append('.');
			out.append(lplpBufStructure.dwFileVersionLS & 0xffff);

			return Optional.of(out.toString());
		}
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	interface Version extends Library
	{
		/**
		 * Get File Version Info buffer size
		 * 
		 * https://learn.microsoft.com/en-us/windows/win32/api/winver/nf-winver-getfileversioninfosizew
		 *
		 * @param lptstrFilename file name
		 * @param dwDummy ignored
		 * @return file size
		 */
		public int GetFileVersionInfoSizeW(String lptstrFilename, int dwDummy); // NOSONAR JNI Bind

		/**
		 * Get File Version Info.
		 * 
		 * https://learn.microsoft.com/en-us/windows/win32/api/winver/nf-winver-getfileversioninfow
		 * 
		 * @param lptstrFilename file name
		 * @param dwHandle file handle
		 * @param dwLen length of data buffer
		 * @param lpData data buffer
		 * @return success
		 */
		public boolean GetFileVersionInfoW(String lptstrFilename, int dwHandle, int dwLen, Pointer lpData); // NOSONAR JNI Bind

		/**
		 * Query for Version info
		 * 
		 * https://learn.microsoft.com/en-us/windows/win32/api/winver/nf-winver-verqueryvaluew
		 * 
		 * @param pBlock Version Info structure
		 * @param lpSubBlock sub block
		 * @param lplpBuffer pointer to buffer to receive data
		 * @param puLen buffer size 
		 * @return success
		 */
		public boolean VerQueryValueW(Pointer pBlock, String lpSubBlock, PointerByReference lplpBuffer, IntByReference puLen); // NOSONAR JNI Bind
	}

	//--------------------------------------------------------------------------------------------------------------------------------
	/**
	 * https://learn.microsoft.com/en-us/windows/win32/api/verrsrc/ns-verrsrc-vs_fixedfileinfo
	 */
	@FieldOrder({"dwSignature", "dwStrucVersion", "dwFileVersionMS", "dwFileVersionLS",
		"dwProductVersionMS", "dwProductVersionLS", "dwFileFlagsMask",
		"dwFileFlags", "dwFileOS", "dwFileType", "dwFileSubtype", "dwFileDateMS",
		"dwFileDateLS"})
	public static class VS_FIXEDFILEINFO extends Structure
	{
		/**
		 * Signature marker
		 */
		public int	dwSignature;		// NOSONAR JNI Bind
		/**
		 * Structure version
		 */
		public int	dwStrucVersion;		// NOSONAR JNI Bind
		/**
		 * The most significant 32 bits of the file's binary version number. 
		 * This member is used with dwFileVersionLS to form a 64-bit value used for numeric comparisons.
		 */
		public int	dwFileVersionMS;	// NOSONAR JNI Bind
		/**
		 * The least significant 32 bits of the file's binary version number. 
		 * This member is used with dwFileVersionMS to form a 64-bit value used for numeric comparisons.
		 */
		public int	dwFileVersionLS;	// NOSONAR JNI Bind
		/**
		 * The most significant 32 bits of the binary version number of the product with which this file was distributed. 
		 * This member is used with dwProductVersionLS to form a 64-bit value used for numeric comparisons.
		 */
		public int	dwProductVersionMS;	// NOSONAR JNI Bind
		/**
		 * The least significant 32 bits of the binary version number of the product with which this file was distributed. 
		 * This member is used with dwProductVersionMS to form a 64-bit value used for numeric comparisons.
		 */
		public int	dwProductVersionLS;	// NOSONAR JNI Bind
		/**
		 * 
		 */
		public int	dwFileFlagsMask;	// NOSONAR JNI Bind
		/**
		 * 
		 */
		public int	dwFileFlags;		// NOSONAR JNI Bind
		/**
		 * 
		 */
		public int	dwFileOS;			// NOSONAR JNI Bind
		/**
		 * 
		 */
		public int	dwFileType;			// NOSONAR JNI Bind
		/**
		 * 
		 */
		public int	dwFileSubtype;		// NOSONAR JNI Bind
		/**
		 * 
		 */
		public int	dwFileDateMS;		// NOSONAR JNI Bind
		/**
		 * 
		 */
		public int	dwFileDateLS;		// NOSONAR JNI Bind

		/**
		 * @param p data
		 */
		public VS_FIXEDFILEINFO(com.sun.jna.Pointer p)
		{
			super(p);
		}
	}
}