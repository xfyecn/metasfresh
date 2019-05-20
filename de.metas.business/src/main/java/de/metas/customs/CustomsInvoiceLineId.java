package de.metas.customs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import de.metas.util.Check;
import de.metas.util.lang.RepoIdAware;

/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2019 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class CustomsInvoiceLineId implements RepoIdAware
{
	@JsonCreator
	public static CustomsInvoiceLineId ofRepoId(final int repoId)
	{
		return new CustomsInvoiceLineId(repoId);
	}

	int repoId;

	private CustomsInvoiceLineId(final int repoId)
	{
		this.repoId = Check.assumeGreaterThanZero(repoId, "C_Customs_Invoice_Line_ID");
	}

	@Override
	@JsonValue
	public int getRepoId()
	{
		return repoId;
	}

	public static int toRepoId(final CustomsInvoiceLineId id)
	{
		return id != null ? id.getRepoId() : -1;
	}
}
