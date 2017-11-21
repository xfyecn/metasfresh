
CREATE VIEW public.MD_Candidate_Stock_v AS 
SELECT DISTINCT ON (M_Product_ID, StorageAttributesKey, M_Warehouse_ID)
	M_Product_ID,
	StorageAttributesKey,
	M_Warehouse_ID,
	DateProjected,
	Qty
FROM MD_Candidate 
WHERE /* these two condidations are in the whereclausee of the index md_candidate_uc_stock */
	IsActive='Y' AND MD_Candidate_Type='STOCK'
ORDER BY     
	M_Product_ID,
	StorageAttributesKey,
    M_Warehouse_ID,
    Dateprojected DESC
;
