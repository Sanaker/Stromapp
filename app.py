from flask import Flask, jsonify
import requests # Still needed for nordpool library if it uses it, or if you make other calls
import datetime
# import io # No longer needed
# import base64 # No longer needed
# import matplotlib # REMOVE
# # matplotlib.use('Agg') # REMOVE
# import matplotlib.pyplot as plt # REMOVE
import pytz
from nordpool import elspot

app = Flask(__file__)

# --- Konfigurasjon ---
AREA = "NO1"
MVA_RATE = 0.25
STROEMSTOTTE_DEKNING = 0.90
STROEMSTOTTE_GRENSE = 0.9375
TIMEZONE = pytz.timezone('Europe/Oslo')

# --- Hjelpefunksjoner for prisberegning ---
def legg_til_mva(pris):
    return pris * (1 + MVA_RATE)

def juster_med_stotte(pris):
    if pris > STROEMSTOTTE_GRENSE:
        stotte = (pris - STROEMSTOTTE_GRENSE) * STROEMSTOTTE_DEKNING
    else:
        stotte = 0
    return pris - stotte

def hent_strompriser_fra_nordpool():
    prices_spot = elspot.Prices(currency="NOK")
    today = datetime.date.today()
    try:
        data = prices_spot.hourly(areas=[AREA], end_date=today)
        if not data.get('areas', {}).get(AREA, {}).get('values'):
            tomorrow = today + datetime.timedelta(days=1)
            data = prices_spot.hourly(areas=[AREA], end_date=tomorrow)
    except Exception as e:
        print(f"Feil ved henting av Nord Pool data: {e}")
        data = {}
    return data

def parse_priser_for_app(data):
    parsed_prices = []
    if AREA in data.get('areas', {}):
        for hour_data in data['areas'][AREA]['values']:
            start_time = hour_data['start'].astimezone(TIMEZONE)
            raw_price_nok_kwh = hour_data['value'] / 1000
            price_with_vat = round(legg_til_mva(raw_price_nok_kwh), 2)
            adjusted_price_with_vat = round(legg_til_mva(juster_med_stotte(raw_price_nok_kwh)), 2)
            parsed_prices.append({
                "time": start_time.strftime("%Y-%m-%d %H:%M"),
                "price": adjusted_price_with_vat,
                # "raw_price": price_with_vat # You can keep or remove this based on app needs
            })
    parsed_prices.sort(key=lambda x: datetime.datetime.strptime(x["time"], "%Y-%m-%d %H:%M"))
    return parsed_prices

def fetch_nordpool_data_for_graph(): # Consider renaming if it's not "for graph" anymore
    nordpool_raw_data = hent_strompriser_fra_nordpool()
    all_prices = parse_priser_for_app(nordpool_raw_data)
    return all_prices

# --- Hovedrute for Android-appen ---
@app.route('/')
def get_data():
    all_price_data = fetch_nordpool_data_for_graph() # Renamed variable for clarity

    if not all_price_data:
        return jsonify([])

    # Directly return the processed list of prices
    # Ensure this list contains dictionaries with "time" and "price"
    # which your parse_priser_for_app function should be doing.
    return jsonify(all_price_data)

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)