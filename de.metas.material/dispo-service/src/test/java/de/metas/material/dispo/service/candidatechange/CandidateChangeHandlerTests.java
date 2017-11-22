package de.metas.material.dispo.service.candidatechange;

import static de.metas.material.event.EventTestHelper.CLIENT_ID;
import static de.metas.material.event.EventTestHelper.NOW;
import static de.metas.material.event.EventTestHelper.ORG_ID;
import static de.metas.material.event.EventTestHelper.PRODUCT_ID;
import static de.metas.material.event.EventTestHelper.WAREHOUSE_ID;
import static de.metas.material.event.EventTestHelper.createProductDescriptor;
import static de.metas.testsupport.MetasfreshAssertions.assertThatModel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.test.AdempiereTestHelper;
import org.adempiere.test.AdempiereTestWatcher;
import org.adempiere.util.Services;
import org.compiere.util.TimeUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

import com.google.common.collect.ImmutableList;

import de.metas.material.dispo.commons.CandidatesQuery;
import de.metas.material.dispo.commons.DispoTestUtils;
import de.metas.material.dispo.commons.RepositoryTestHelper;
import de.metas.material.dispo.commons.candidate.Candidate;
import de.metas.material.dispo.commons.candidate.CandidateType;
import de.metas.material.dispo.commons.candidate.DemandDetail;
import de.metas.material.dispo.commons.repository.CandidateRepositoryRetrieval;
import de.metas.material.dispo.commons.repository.CandidateRepositoryWriteService;
import de.metas.material.dispo.commons.repository.StockRepository;
import de.metas.material.dispo.model.I_MD_Candidate;
import de.metas.material.dispo.service.candidatechange.handler.CandidateHandler;
import de.metas.material.dispo.service.candidatechange.handler.DemandCandiateHandler;
import de.metas.material.dispo.service.candidatechange.handler.SupplyCandiateHandler;
import de.metas.material.event.MaterialEventService;
import de.metas.material.event.commons.MaterialDescriptor;
import de.metas.material.event.commons.MaterialDescriptor.DateOperator;
import lombok.NonNull;
import mockit.Mocked;

/*
 * #%L
 * metasfresh-manufacturing-dispo
 * %%
 * Copyright (C) 2017 metas GmbH
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

public class CandidateChangeHandlerTests
{
	/** Watches the current tests and dumps the database to console in case of failure */
	@Rule
	public final TestWatcher testWatcher = new AdempiereTestWatcher();

	private final Date t1 = TimeUtil.parseTimestamp("2017-11-22 00:00");
	private final Date t2 = TimeUtil.addMinutes(t1, 10);
	private final Date t3 = TimeUtil.addMinutes(t1, 20);
	private final Date t4 = TimeUtil.addMinutes(t1, 30);

	private final int OTHER_WAREHOUSE_ID = WAREHOUSE_ID + 10;

	private CandidateRepositoryRetrieval candidateRepositoryRetrieval;

	private StockRepository stockRepository;

	private CandidateChangeService candidateChangeHandler;

	@Mocked
	private MaterialEventService materialEventService;

	private StockCandidateService stockCandidateService;

	private CandidateRepositoryWriteService candidateRepositoryCommands;

	@Before
	public void init()
	{
		AdempiereTestHelper.get().init();

		candidateRepositoryRetrieval = new CandidateRepositoryRetrieval();
		candidateRepositoryCommands = new CandidateRepositoryWriteService();

		stockRepository = new StockRepository();
		stockCandidateService = new StockCandidateService(candidateRepositoryRetrieval, candidateRepositoryCommands);

		candidateChangeHandler = new CandidateChangeService(
				ImmutableList.of(
						new DemandCandiateHandler(candidateRepositoryRetrieval, candidateRepositoryCommands, materialEventService, stockRepository, stockCandidateService),
						new SupplyCandiateHandler(candidateRepositoryRetrieval, candidateRepositoryCommands, stockCandidateService)));
	}

	@Test
	public void createMapOfHandlers()
	{
		final CandidateHandler handler1 = createHandlerThatSupportsTypes(ImmutableList.of(CandidateType.DEMAND, CandidateType.SUPPLY));
		final CandidateHandler handler2 = createHandlerThatSupportsTypes(ImmutableList.of(CandidateType.STOCK_UP, CandidateType.UNRELATED_DECREASE));

		final Map<CandidateType, CandidateHandler> result = CandidateChangeService.createMapOfHandlers(ImmutableList.of(handler1, handler2));
		assertThat(result).hasSize(4);
		assertThat(result.get(CandidateType.DEMAND)).isSameAs(handler1);
		assertThat(result.get(CandidateType.SUPPLY)).isSameAs(handler1);
		assertThat(result.get(CandidateType.STOCK_UP)).isSameAs(handler2);
		assertThat(result.get(CandidateType.UNRELATED_DECREASE)).isSameAs(handler2);
	}

	@Test(expected = RuntimeException.class)
	public void createMapOfHandlers_when_typeColission_then_exception()
	{
		final CandidateHandler handler1 = createHandlerThatSupportsTypes(ImmutableList.of(CandidateType.DEMAND, CandidateType.SUPPLY));
		final CandidateHandler handler2 = createHandlerThatSupportsTypes(ImmutableList.of(CandidateType.DEMAND, CandidateType.UNRELATED_DECREASE));

		CandidateChangeService.createMapOfHandlers(ImmutableList.of(handler1, handler2));
	}

	private CandidateHandler createHandlerThatSupportsTypes(final ImmutableList<CandidateType> types)
	{
		return new CandidateHandler()
		{
			@Override
			public Candidate onCandidateNewOrChange(Candidate candidate)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public Collection<CandidateType> getHandeledTypes()
			{
				return types;
			}
		};
	}

	/**
	 * Verifies that {@link CandidateChangeService#applyDeltaToLaterStockCandidates(CandidatesQuery, BigDecimal)} applies the given delta to the right records.
	 * Only records that have a <i>different</i> M_Warenhouse_ID shall not be touched.
	 */
	@Test
	public void testApplyDeltaToLaterStockCandidates()
	{
		final Candidate earlierCandidate;
		final Candidate candidate;
		final Candidate evenLaterCandidate;
		final Candidate evenLaterCandidateWithDifferentWarehouse;

		// preparations
		{
			final MaterialDescriptor materialDescriptor = MaterialDescriptor.builder()
					.complete(true)
					.productDescriptor(createProductDescriptor())
					.warehouseId(WAREHOUSE_ID)
					.quantity(new BigDecimal("10"))
					.date(t2)
					.build();

			candidate = Candidate.builder()
					.type(CandidateType.STOCK)
					.clientId(CLIENT_ID)
					.orgId(ORG_ID)
					.materialDescriptor(materialDescriptor)
					.build();
			candidateRepositoryCommands.addOrUpdateOverwriteStoredSeqNo(candidate);

			final MaterialDescriptor earlierMaterialDescriptor = materialDescriptor.withDate(t1);

			earlierCandidate = candidateRepositoryCommands
					.addOrUpdateOverwriteStoredSeqNo(Candidate.builder()
							.type(CandidateType.STOCK)
							.clientId(CLIENT_ID)
							.orgId(ORG_ID)
							.materialDescriptor(earlierMaterialDescriptor)
							.build());

			final MaterialDescriptor laterMaterialDescriptor = materialDescriptor.withDate(t3);

			final Candidate laterCandidate = Candidate.builder()
					.type(CandidateType.STOCK)
					.clientId(CLIENT_ID)
					.orgId(ORG_ID)
					.materialDescriptor(laterMaterialDescriptor)
					.build();
			candidateRepositoryCommands.addOrUpdateOverwriteStoredSeqNo(laterCandidate);

			final MaterialDescriptor evenLatermaterialDescriptor = materialDescriptor
					.withQuantity(new BigDecimal("12"))
					.withDate(t4);

			evenLaterCandidate = Candidate.builder()
					.type(CandidateType.STOCK)
					.clientId(CLIENT_ID)
					.orgId(ORG_ID)
					.materialDescriptor(evenLatermaterialDescriptor)
					.build();
			candidateRepositoryCommands.addOrUpdateOverwriteStoredSeqNo(evenLaterCandidate);

			final MaterialDescriptor evenLatermaterialDescrWithDifferentWarehouse = evenLatermaterialDescriptor
					.withWarehouseId(OTHER_WAREHOUSE_ID);

			evenLaterCandidateWithDifferentWarehouse = Candidate.builder()
					.type(CandidateType.STOCK)
					.clientId(CLIENT_ID)
					.orgId(ORG_ID)
					.materialDescriptor(evenLatermaterialDescrWithDifferentWarehouse)
					.build();
			candidateRepositoryCommands.addOrUpdateOverwriteStoredSeqNo(evenLaterCandidateWithDifferentWarehouse);
		}

		// do the test
		final MaterialDescriptor materialDescriptor = MaterialDescriptor.builderForQuery()
				.productDescriptor(createProductDescriptor())
				.warehouseId(WAREHOUSE_ID)
				.date(t2)
				.build();
		stockCandidateService.applyDeltaToMatchingLaterStockCandidates(
				materialDescriptor,
				earlierCandidate.getGroupId(),
				new BigDecimal("3"));

		// assert that every stock record got some groupId
		assertThat(DispoTestUtils.retrieveAllRecords()).allSatisfy(r -> assertThatModel(r).hasValueGreaterThanZero(I_MD_Candidate.COLUMN_MD_Candidate_GroupId));

		final Candidate earlierCandidateAfterChange = candidateRepositoryRetrieval.retrieveLatestMatchOrNull(mkQueryForStockUntilDate(t1, WAREHOUSE_ID));
		assertThat(earlierCandidateAfterChange).as("Expected canddiate with Date=<%s> and warehouseId=<%s> to exist", t1, WAREHOUSE_ID).isNotNull();
		assertThat(earlierCandidateAfterChange.getQuantity()).isEqualTo(earlierCandidate.getQuantity()); // quantity shall be unchanged
		assertThat(earlierCandidateAfterChange.getGroupId()).isEqualTo(earlierCandidate.getGroupId()); // basically the same candidate

		final I_MD_Candidate candidateRecordAfterChange = DispoTestUtils.filter(CandidateType.STOCK, t2).get(0); // candidateRepository.retrieveExact(candidate).get();
		assertThat(candidateRecordAfterChange.getQty()).isEqualByComparingTo("10"); // quantity shall be unchanged, because that method shall only update *later* records
		assertThat(candidateRecordAfterChange.getMD_Candidate_GroupId(), not(is(earlierCandidate.getGroupId())));

		final Candidate laterCandidateAfterChange = candidateRepositoryRetrieval.retrieveLatestMatchOrNull(mkQueryForStockUntilDate(t3, WAREHOUSE_ID));
		assertThat(laterCandidateAfterChange).isNotNull();
		assertThat(laterCandidateAfterChange.getQuantity()).isEqualByComparingTo("13"); // quantity shall be plus 3
		assertThat(laterCandidateAfterChange.getGroupId()).isEqualTo(earlierCandidate.getGroupId());

		final I_MD_Candidate evenLaterCandidateRecordAfterChange = DispoTestUtils.filter(CandidateType.STOCK, t4, PRODUCT_ID, WAREHOUSE_ID).get(0); // candidateRepository.retrieveExact(evenLaterCandidate).get();
		assertThat(evenLaterCandidateRecordAfterChange.getQty()).isEqualByComparingTo("15"); // quantity shall be plus 3 too
		assertThat(evenLaterCandidateRecordAfterChange.getMD_Candidate_GroupId()).isEqualTo(earlierCandidate.getGroupId());

		final I_MD_Candidate evenLaterCandidateWithDifferentWarehouseAfterChange = DispoTestUtils.filter(CandidateType.STOCK, t4, PRODUCT_ID, OTHER_WAREHOUSE_ID).get(0); // candidateRepository.retrieveExact(evenLaterCandidateWithDifferentWarehouse).get();
		assertThat(evenLaterCandidateWithDifferentWarehouseAfterChange.getQty()).isEqualByComparingTo("12"); // quantity shall be unchanged, because we changed another warehouse and this one should not have been matched
		assertThat(evenLaterCandidateWithDifferentWarehouseAfterChange.getMD_Candidate_GroupId(), not(is(earlierCandidate.getGroupId())));

	}

	private CandidatesQuery mkQueryForStockUntilDate(@NonNull final Date timestamp, final int warehouseId)
	{
		return CandidatesQuery.builder()
				.type(CandidateType.STOCK)
				.materialDescriptor(MaterialDescriptor.builderForQuery()
						.productDescriptor(createProductDescriptor())
						.warehouseId(warehouseId)
						.date(timestamp)
						.dateOperator(DateOperator.BEFORE_OR_AT)
						.build())
				.parentId(CandidatesQuery.UNSPECIFIED_PARENT_ID)
				.build();
	}

	@Test
	public void testUpdateStockDifferentTimes()
	{
		invokeAddOrUpdateStock(t1, "10");
		invokeAddOrUpdateStock(t4, "2");
		invokeAddOrUpdateStock(t3, "-3");
		invokeAddOrUpdateStock(t2, "-4");

		final List<I_MD_Candidate> records = retrieveAllRecordsSorted();
		assertThat(records).hasSize(4);

		assertThat(records.get(0).getDateProjected().getTime()).isEqualTo(t1.getTime());
		assertThat(records.get(0).getQty()).isEqualByComparingTo("10");

		assertThat(records.get(1).getDateProjected().getTime()).isEqualTo(t2.getTime());
		assertThat(records.get(1).getQty()).isEqualByComparingTo("6");

		assertThat(records.get(2).getDateProjected().getTime()).isEqualTo(t3.getTime());
		assertThat(records.get(2).getQty()).isEqualByComparingTo("3");

		assertThat(records.get(3).getDateProjected().getTime()).isEqualTo(t4.getTime());
		assertThat(records.get(3).getQty()).isEqualByComparingTo("5");

		// all these stock records need to have the same group-ID
		final int groupId = records.get(0).getMD_Candidate_GroupId();
		assertThat(groupId, greaterThan(0));
		records.forEach(r -> assertThat(r.getMD_Candidate_GroupId()).isEqualTo(groupId));
	}

	private Candidate invokeAddOrUpdateStock(@NonNull final Date date, @NonNull final String qty)
	{
		final MaterialDescriptor materialDescr = MaterialDescriptor.builderForCompleteDescriptor()
				.productDescriptor(createProductDescriptor())
				.warehouseId(WAREHOUSE_ID)
				.quantity(new BigDecimal(qty))
				.date(date)
				.build();

		final Candidate candidate = Candidate.builder()
				.type(CandidateType.STOCK)
				.clientId(CLIENT_ID)
				.orgId(ORG_ID)
				.materialDescriptor(materialDescr)
				.build();
		final Candidate processedCandidate = stockCandidateService.addOrUpdateStock(candidate);
		return processedCandidate;
	}

	/**
	 * Similar to {@link #testUpdateStockDifferentTimes()}, but two invocations have the same timestamp.
	 */
	@Test
	public void addOrUpdateStock_With_Overlapping_Time()
	{
		{
			invokeAddOrUpdateStock(t1, "10");

			final List<I_MD_Candidate> records = retrieveAllRecordsSorted();
			assertThat(records).hasSize(1);
			assertThat(records.get(0).getDateProjected().getTime()).isEqualTo(t1.getTime());
			assertThat(records.get(0).getQty()).isEqualByComparingTo("10");
		}

		{
			invokeAddOrUpdateStock(t4, "2");

			final List<I_MD_Candidate> records = retrieveAllRecordsSorted();
			assertThat(records).hasSize(2);
			assertThat(records.get(0).getDateProjected().getTime()).isEqualTo(t1.getTime());
			assertThat(records.get(0).getQty()).isEqualByComparingTo("10");
			assertThat(records.get(1).getDateProjected().getTime()).isEqualTo(t4.getTime());
			assertThat(records.get(1).getQty()).isEqualByComparingTo("12");
		}

		{
			invokeAddOrUpdateStock(t3, "-3");

			final List<I_MD_Candidate> records = retrieveAllRecordsSorted();
			assertThat(records).hasSize(3);

			assertThat(records.get(0).getDateProjected().getTime()).isEqualTo(t1.getTime());
			assertThat(records.get(0).getQty()).isEqualByComparingTo("10");
			assertThat(records.get(1).getDateProjected().getTime()).isEqualTo(t3.getTime());
			assertThat(records.get(1).getQty()).isEqualByComparingTo("7");
			assertThat(records.get(2).getDateProjected().getTime()).isEqualTo(t4.getTime());
			assertThat(records.get(2).getQty()).isEqualByComparingTo("9");
		}

		{
			invokeAddOrUpdateStock(t3, "-4"); // same time again!

			final List<I_MD_Candidate> records = retrieveAllRecordsSorted();
			assertThat(records).hasSize(3);

			assertThat(records.get(0).getDateProjected().getTime()).isEqualTo(t1.getTime());
			assertThat(records.get(0).getQty()).isEqualByComparingTo("10");

			assertThat(records.get(1).getDateProjected().getTime()).isEqualTo(t3.getTime());
			assertThat(records.get(1).getQty()).isEqualByComparingTo("3");

			assertThat(records.get(2).getDateProjected().getTime()).isEqualTo(t4.getTime());
			assertThat(records.get(2).getQty()).isEqualByComparingTo("5");
		}

		// all these stock records need to have the same group-ID
		final List<I_MD_Candidate> records = retrieveAllRecordsSorted();
		assertThatModel(records.get(0)).hasValueGreaterThanZero(I_MD_Candidate.COLUMN_MD_Candidate_GroupId);

		final int groupId = records.get(0).getMD_Candidate_GroupId();
		assertThat(records).allSatisfy(r -> assertThatModel(r).hasNonNullValue(I_MD_Candidate.COLUMN_MD_Candidate_GroupId, groupId));
	}

	public List<I_MD_Candidate> retrieveAllRecordsSorted()
	{
		final IQueryBL queryBL = Services.get(IQueryBL.class);
		return queryBL
				.createQueryBuilder(I_MD_Candidate.class)
				.orderBy()
				.addColumn(I_MD_Candidate.COLUMN_DateProjected)
				.addColumn(I_MD_Candidate.COLUMN_MD_Candidate_ID)
				.endOrderBy()
				.create()
				.list();
	}

	/**
	 * Verifies that {@link CandidateChangeService#addOrUpdateStock(Candidate)} also works if the candidate we update with is not a stock candidate.
	 */
	@Test
	public void testOnStockCandidateNewOrChangedNotStockType()
	{
		final MaterialDescriptor materialDescr = MaterialDescriptor.builder()
				.complete(true)
				.productDescriptor(createProductDescriptor())
				.warehouseId(WAREHOUSE_ID)
				.quantity(new BigDecimal("10"))
				.date(t2)
				.build();

		final Candidate candidate = Candidate.builder()
				.type(CandidateType.SUPPLY)
				.clientId(CLIENT_ID)
				.orgId(ORG_ID)
				.materialDescriptor(materialDescr)
				.build();

		final Candidate processedCandidate = stockCandidateService.addOrUpdateStock(candidate);
		assertThat(processedCandidate.getType()).isEqualTo(CandidateType.STOCK);
		assertThat(processedCandidate.getMaterialDescriptor().getDate().getTime()).isEqualTo(t2.getTime());
		assertThat(processedCandidate.getMaterialDescriptor().getQuantity()).isEqualByComparingTo(BigDecimal.TEN);
		assertThat(processedCandidate.getMaterialDescriptor().getProductId()).isEqualTo(PRODUCT_ID);
		assertThat(processedCandidate.getMaterialDescriptor().getWarehouseId()).isEqualTo(WAREHOUSE_ID);
	}

	/**
	 * Similar to testOnDemandCandidateCandidateNewOrChange_noOlderRecords, but then adds an accompanying demand and verifies the SeqNo values
	 */
	@Test
	public void onCandidateNewOrChange_demand_then_unrelated_supply()
	{
		final BigDecimal qty = new BigDecimal("23");

		createAndAddDemandWithQtyandDemandDetail(qty, 20);
		// we don't really check here..this first part is already verified in testOnDemandCandidateCandidateNewOrChange_noOlderRecords()
		assertThat(DispoTestUtils.retrieveAllRecords()).hasSize(2); // one demand, one stock
		assertThat(DispoTestUtils.filter(CandidateType.STOCK).get(0).getQty()).isEqualByComparingTo("-23");

		createAndAddSupplyWithQtyandDemandDetail(qty, 30);
		{
			final List<I_MD_Candidate> records = DispoTestUtils.retrieveAllRecords();
			// we need one demand, one supply and *two* different stocks, since demand and supply are not related
			assertThat(records).hasSize(4);

			final I_MD_Candidate demandRecord = DispoTestUtils.filter(CandidateType.DEMAND).get(0);
			final I_MD_Candidate firstStockRecord = DispoTestUtils.filter(CandidateType.STOCK).get(0);

			final I_MD_Candidate supplyRecord = DispoTestUtils.filter(CandidateType.SUPPLY).get(0);
			final I_MD_Candidate secondStockRecord = DispoTestUtils.filter(CandidateType.STOCK).get(1);

			assertThatModel(firstStockRecord).hasNonNullValue(I_MD_Candidate.COLUMN_SeqNo, demandRecord.getSeqNo() + 1);

			assertThatModel(supplyRecord).hasNonNullValue(I_MD_Candidate.COLUMN_SeqNo, secondStockRecord.getSeqNo() + 1);  // as before

			// shall be balanced between the demand and the supply
			assertThatModel(secondStockRecord).hasNonNullValue(I_MD_Candidate.COLUMN_DateProjected, firstStockRecord.getDateProjected());
			assertThat(secondStockRecord.getQty()).isEqualByComparingTo("23");
			assertThat(firstStockRecord.getQty()).isEqualByComparingTo("-23");
		}
	}

	@Test
	public void onCandidateNewOrChange_demand_then_related_supply()
	{
		final BigDecimal qty = new BigDecimal("23");

		createAndAddDemandWithQtyandDemandDetail(qty, 20);
		// we don't really check here..this first part is already verified in testOnDemandCandidateCandidateNewOrChange_noOlderRecords()
		assertThat(DispoTestUtils.retrieveAllRecords()).hasSize(2); // one demand, one stock
		assertThat(DispoTestUtils.filter(CandidateType.STOCK).get(0).getQty()).isEqualByComparingTo("-23");

		createAndAddSupplyWithQtyandDemandDetail(qty, 20);
		{
			final List<I_MD_Candidate> records = DispoTestUtils.retrieveAllRecords();
			// we need one demand, one supply and *two* different stocks, since demand and supply are not related
			assertThat(records).hasSize(3);

			final I_MD_Candidate demandRecord = DispoTestUtils.filter(CandidateType.DEMAND).get(0);
			final I_MD_Candidate firstStockRecord = DispoTestUtils.filter(CandidateType.STOCK).get(0);
			final I_MD_Candidate supplyRecord = DispoTestUtils.filter(CandidateType.SUPPLY).get(0);

			assertThatModel(firstStockRecord).hasNonNullValue(I_MD_Candidate.COLUMN_MD_Candidate_Parent_ID, demandRecord.getMD_Candidate_ID());
			assertThatModel(supplyRecord).hasNonNullValue(I_MD_Candidate.COLUMN_MD_Candidate_Parent_ID, firstStockRecord.getMD_Candidate_ID());

			assertThatModel(firstStockRecord).hasNonNullValue(I_MD_Candidate.COLUMN_SeqNo, demandRecord.getSeqNo() + 1);
			assertThatModel(supplyRecord).hasNonNullValue(I_MD_Candidate.COLUMN_SeqNo, firstStockRecord.getSeqNo() + 1);

			// assertThatModel(supplyRecord).hasNonNullValue(I_MD_Candidate.COLUMN_SeqNo, secondStockRecord.getSeqNo() + 1); // as before

			// shall be balanced between the demand and the supply
			// assertThatModel(secondStockRecord).hasNonNullValue(I_MD_Candidate.COLUMN_DateProjected, firstStockRecord.getDateProjected());
			// assertThat(secondStockRecord.getQty()).isEqualByComparingTo("23");
			assertThat(firstStockRecord.getQty()).isEqualByComparingTo("0");
		}
	}

	private void createAndAddDemandWithQtyandDemandDetail(
			@NonNull final BigDecimal qty,
			final int shipmentScheduleIdForDemandDetail)
	{
		final MaterialDescriptor materialDescr = MaterialDescriptor.builder()
				.complete(true)
				.productDescriptor(createProductDescriptor())
				.warehouseId(WAREHOUSE_ID)
				.quantity(qty)
				.date(NOW)
				.build();

		RepositoryTestHelper.setupMockedRetrieveAvailableStock(
				stockRepository,
				materialDescr,
				"0");

		final Candidate candidate = Candidate.builder()
				.type(CandidateType.DEMAND)
				.clientId(CLIENT_ID)
				.orgId(ORG_ID)
				.materialDescriptor(materialDescr)
				.demandDetail(DemandDetail.forShipmentScheduleIdAndOrderLineId(shipmentScheduleIdForDemandDetail, 0))
				.build();
		candidateChangeHandler.onCandidateNewOrChange(candidate);
	}

	private void createAndAddSupplyWithQtyandDemandDetail(
			@NonNull final BigDecimal qty,
			final int shipmentScheduleIdForDemandDetail)
	{
		final MaterialDescriptor supplyMaterialDescriptor = MaterialDescriptor.builder()
				.complete(true)
				.productDescriptor(createProductDescriptor())
				.warehouseId(WAREHOUSE_ID)
				.quantity(qty)
				.date(NOW)
				.build();

		final Candidate supplyCandidate = Candidate.builder()
				.type(CandidateType.SUPPLY)
				.clientId(CLIENT_ID)
				.orgId(ORG_ID)
				.materialDescriptor(supplyMaterialDescriptor)
				.demandDetail(DemandDetail.forShipmentScheduleIdAndOrderLineId(shipmentScheduleIdForDemandDetail, 0))
				.build();

		candidateChangeHandler.onCandidateNewOrChange(supplyCandidate);
	}

	/**
	 * Similar to {@link #testDemand_Then_UnrelatedSupply()}, but this time, we first add the supply candidate.
	 * Therefore its {@link I_MD_Candidate} records gets to be persisted first. still, the {@code SeqNo} needs to be "stable".
	 */
	@Test
	public void onCandidateNewOrChange_supply_then_unrelated_demand()
	{
		final BigDecimal qty = new BigDecimal("23");

		createAndAddSupplyWithQtyandDemandDetail(qty, 20);
		assertThat(DispoTestUtils.filter(CandidateType.STOCK)).hasSize(1);
		assertThat(DispoTestUtils.filter(CandidateType.STOCK).get(0).getQty()).isEqualByComparingTo("23");

		{
			assertThat(DispoTestUtils.retrieveAllRecords()).hasSize(2); // one supply, one stock

			final I_MD_Candidate stockRecord = DispoTestUtils.filter(CandidateType.STOCK).get(0);
			final I_MD_Candidate supplyRecord = DispoTestUtils.filter(CandidateType.SUPPLY).get(0);

			assertThatModel(supplyRecord).hasNonNullValue(I_MD_Candidate.COLUMN_MD_Candidate_Parent_ID, stockRecord.getMD_Candidate_ID());
			assertThatModel(supplyRecord).hasNonNullValue(I_MD_Candidate.COLUMN_SeqNo, stockRecord.getSeqNo() + 1);
		}

		createAndAddDemandWithQtyandDemandDetail(qty, 30);
		{
			assertThat(DispoTestUtils.retrieveAllRecords()).hasSize(4);

			final List<I_MD_Candidate> allStockCandidates = DispoTestUtils.filter(CandidateType.STOCK);
			assertThat(allStockCandidates).hasSize(2);

			final I_MD_Candidate supplyRecord = DispoTestUtils.filter(CandidateType.SUPPLY).get(0);
			final I_MD_Candidate firstStockRecord = allStockCandidates.get(0);

			final I_MD_Candidate demandRecord = DispoTestUtils.filter(CandidateType.DEMAND).get(0);
			final I_MD_Candidate secondStockRecord = allStockCandidates.get(1);

			assertThatModel(supplyRecord).hasNonNullValue(I_MD_Candidate.COLUMN_SeqNo, firstStockRecord.getSeqNo() + 1);  // as before
			assertThatModel(secondStockRecord).hasNonNullValue(I_MD_Candidate.COLUMN_SeqNo, demandRecord.getSeqNo() + 1);

			// shall both be balanced between the demand and the supply
			assertThatModel(firstStockRecord).hasNonNullValue(I_MD_Candidate.COLUMN_DateProjected, secondStockRecord.getDateProjected());
			assertThat(firstStockRecord.getQty()).isEqualByComparingTo("23");
			assertThat(secondStockRecord.getQty()).isEqualByComparingTo("-23");
		}
	}

	// tODO make it work for supply and demand that are related via demanddetail

	/**
	 * Like {@link #testOnDemandCandidateCandidateNewOrChange_noOlderRecords()},
	 * but the method under test is called two times. We expect the code to recognize this and not count the 2nd invocation.
	 */
	@Test
	public void testOnDemandCandidateCandidateNewOrChange_noOlderRecords_invokeTwiceWithSame()
	{
		final BigDecimal qty = new BigDecimal("23");

		final MaterialDescriptor materialDescriptor = MaterialDescriptor.builder()
				.complete(true)
				.productDescriptor(createProductDescriptor())
				.warehouseId(WAREHOUSE_ID)
				.quantity(qty)
				.date(NOW)
				.build();

		RepositoryTestHelper.setupMockedRetrieveAvailableStock(
				stockRepository,
				materialDescriptor,
				"0");

		final Candidate candidate = Candidate.builder()
				.type(CandidateType.DEMAND)
				.clientId(CLIENT_ID)
				.orgId(ORG_ID)
				.materialDescriptor(materialDescriptor)
				.build();

		final Consumer<Candidate> doTest = candidateUnderTest -> {

			candidateChangeHandler.onCandidateNewOrChange(candidateUnderTest);

			final List<I_MD_Candidate> records = DispoTestUtils.retrieveAllRecords();
			assertThat(records).hasSize(2);
			final I_MD_Candidate stockRecord = DispoTestUtils.filter(CandidateType.STOCK).get(0);
			final I_MD_Candidate demandRecord = DispoTestUtils.filter(CandidateType.DEMAND).get(0);

			assertThat(demandRecord.getQty()).isEqualByComparingTo(qty);
			assertThat(stockRecord.getQty()).isEqualByComparingTo(qty.negate()); // ..because there was no older record, the "delta" we provided is the current quantity
			assertThat(stockRecord.getMD_Candidate_Parent_ID()).isEqualTo(demandRecord.getMD_Candidate_ID());

			assertThat(stockRecord.getSeqNo()).isEqualTo(demandRecord.getSeqNo() + 1); // when we sort by SeqNo, the demand needs to be first and thus have a smaller value
		};

		doTest.accept(candidate); // first invocation
		doTest.accept(candidate); // second invocation
	}

	/**
	 * like {@link #testOnDemandCandidateCandidateNewOrChange_noOlderRecords_invokeTwiceWitDifferent()},
	 * but on the 2nd invocation, a different demand-quantity is used.
	 */
	@Test
	public void testOnDemandCandidateCandidateNewOrChange_noOlderRecords_invokeTwiceWitDifferent()
	{
		final BigDecimal qty = new BigDecimal("23");
		final Date t = t1;

		final MaterialDescriptor materialDescriptor = MaterialDescriptor.builder()
				.complete(true)
				.productDescriptor(createProductDescriptor())
				.warehouseId(WAREHOUSE_ID)
				.quantity(qty)
				.date(t)
				.build();

		RepositoryTestHelper.setupMockedRetrieveAvailableStock(
				stockRepository,
				materialDescriptor,
				"0");

		final Candidate candidate = Candidate.builder()
				.type(CandidateType.DEMAND)
				.clientId(CLIENT_ID)
				.orgId(ORG_ID)
				.materialDescriptor(materialDescriptor)
				.build();

		final BiConsumer<Candidate, BigDecimal> doTest = (candidateUnderTest, expectedQty) -> {
			candidateChangeHandler.onCandidateNewOrChange(candidateUnderTest);

			final List<I_MD_Candidate> records = DispoTestUtils.retrieveAllRecords();
			assertThat(records).hasSize(2);
			final I_MD_Candidate stockRecord = DispoTestUtils.filter(CandidateType.STOCK).get(0);
			final I_MD_Candidate demandRecord = DispoTestUtils.filter(CandidateType.DEMAND).get(0);

			assertThat(demandRecord.getQty()).isEqualByComparingTo(expectedQty);
			assertThat(stockRecord.getQty()).isEqualByComparingTo(expectedQty.negate()); // ..because there was no older record, the "delta" we provided is the current quantity
			assertThat(stockRecord.getMD_Candidate_Parent_ID()).isEqualTo(demandRecord.getMD_Candidate_ID());

			assertThat(stockRecord.getSeqNo()).isEqualTo(demandRecord.getSeqNo() + 1); // when we sort by SeqNo, the demand needs to be first and thus have the smaller number
		};

		doTest.accept(candidate, qty); // first invocation
		doTest.accept(candidate.withQuantity(qty.add(BigDecimal.ONE)), qty.add(BigDecimal.ONE)); // second invocation
	}

}
