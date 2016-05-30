-- 1 ( 13422 )
select count(*) from (
select userID from Sellers
UNION
select userID from Bidders
) as users;

-- 2 ( 103 rows )
select count(*) from Items where BINARY location = "New York";

-- 3 ( 8365 rows )
select count(*) from (
select itemID from ItemHasCategory group by itemID having count(*) = 4
) as auctions;

-- 4 ( itemID = 1046740686 ) including solution for multiple Max values
/*
solution only for 1 MAX 
select itemID from Items where numberOfBids > 0 order by currently desc limit 1
*/
select itemID from Items where currently = (
select MAX(currently) from Items where numberOfBids > 0 and started < "2001-12-20 00:00:00" and 
ends > "2001-12-20 00:00:01"
) and numberOfBids > 0  and started < "2001-12-20 00:00:00" and ends > "2001-12-20 00:00:01";

-- 5 ( 3130 )
select count(*) from Sellers where rating > 1000;

-- 6 ( 6717 )
select count(userID) from Sellers where userID in (select userID from Bidders);

-- 7 ( 150 )
/* exists means has at least one for better performance, we also don't need the table categories because we care only
about the number so the table ItemHasCategory(categoryID,itemID) is enough*/

select  count( distinct ihc.categoryID) from Items item,ItemHasBid ihb, ItemHasCategory ihc where 
item.itemID = ihb.itemID and
exists(
select * from Bids bid where bid.bidID = ihb.bidID and bid.amount > 100
) 
and ihc.itemID = item.itemID;







