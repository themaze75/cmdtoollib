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
package com.maziade.cmdtool.data;

import java.util.Map;
import org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle;
import org.apache.commons.lang3.builder.ToStringBuilder;

//--------------------------------------------------------------------------------------------------------------------------------
public record VersionInfo(
		String version,
		Map<String, String> meta)
{
	//--------------------------------------------------------------------------------------------------------------------------------
	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this, new MultilineRecursiveToStringStyle());
	}
}
