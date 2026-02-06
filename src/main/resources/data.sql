-- Sample Bets Data for Testing
-- Event: EVT-001 (Football Match: Team A vs Team B)

-- Bets predicting Team A will win
INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-001', 'EVT-001', 'MATCH_WINNER', 'TEAM-A', 100.00, 'PENDING', CURRENT_TIMESTAMP);

INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-002', 'EVT-001', 'MATCH_WINNER', 'TEAM-A', 50.00, 'PENDING', CURRENT_TIMESTAMP);

INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-003', 'EVT-001', 'MATCH_WINNER', 'TEAM-A', 250.00, 'PENDING', CURRENT_TIMESTAMP);

-- Bets predicting Team B will win
INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-004', 'EVT-001', 'MATCH_WINNER', 'TEAM-B', 75.00, 'PENDING', CURRENT_TIMESTAMP);

INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-005', 'EVT-001', 'MATCH_WINNER', 'TEAM-B', 150.00, 'PENDING', CURRENT_TIMESTAMP);

INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-006', 'EVT-001', 'MATCH_WINNER', 'TEAM-B', 200.00, 'PENDING', CURRENT_TIMESTAMP);

-- Different market types for the same event
INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-007', 'EVT-001', 'OVER_UNDER_2_5', 'OVER', 80.00, 'PENDING', CURRENT_TIMESTAMP);

INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-008', 'EVT-001', 'BOTH_TEAMS_TO_SCORE', 'YES', 60.00, 'PENDING', CURRENT_TIMESTAMP);

-- Event: EVT-002 (Basketball Game: Lakers vs Celtics)
INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-009', 'EVT-002', 'MATCH_WINNER', 'LAKERS', 120.00, 'PENDING', CURRENT_TIMESTAMP);

INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-010', 'EVT-002', 'MATCH_WINNER', 'CELTICS', 90.00, 'PENDING', CURRENT_TIMESTAMP);

-- Event: EVT-003 (Tennis Match: Federer vs Nadal)
INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-011', 'EVT-003', 'MATCH_WINNER', 'FEDERER', 300.00, 'PENDING', CURRENT_TIMESTAMP);

INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-012', 'EVT-003', 'MATCH_WINNER', 'NADAL', 275.00, 'PENDING', CURRENT_TIMESTAMP);

-- Some already settled bets (for testing queries)
INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at, settled_at) 
VALUES ('USER-013', 'EVT-099', 'MATCH_WINNER', 'TEAM-X', 100.00, 'WON', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY);

INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at, settled_at) 
VALUES ('USER-014', 'EVT-099', 'MATCH_WINNER', 'TEAM-Y', 150.00, 'LOST', CURRENT_TIMESTAMP - INTERVAL '2' DAY, CURRENT_TIMESTAMP - INTERVAL '1' DAY);

-- High-value bets
INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-015', 'EVT-001', 'MATCH_WINNER', 'TEAM-A', 1000.00, 'PENDING', CURRENT_TIMESTAMP);

INSERT INTO bet (user_id, event_id, event_market_id, event_winner_id, bet_amount, status, created_at) 
VALUES ('USER-016', 'EVT-001', 'MATCH_WINNER', 'TEAM-B', 850.00, 'PENDING', CURRENT_TIMESTAMP);

-- Notes:
-- To test the settlement flow, send a POST request with:
-- Event EVT-001 with eventWinnerId = 'TEAM-A' -> Should settle bets for USER-001, USER-002, USER-003, USER-015 as WON
--                                                 and USER-004, USER-005, USER-006, USER-016 as LOST
-- Event EVT-002 with eventWinnerId = 'LAKERS' -> Should settle USER-009 as WON and USER-010 as LOST
-- Event EVT-003 with eventWinnerId = 'NADAL' -> Should settle USER-012 as WON and USER-011 as LOST
