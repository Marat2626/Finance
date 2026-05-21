# import json
# import pandas as pd
# from prophet import Prophet
#
# def predict_income(json_data):
#     data = json.loads(json_data)
#
#     df = pd.DataFrame({
#         'ds': pd.to_datetime([f'2024-{int(m):02d}-01' for m in data['months']]),
#         'y': data['income']
#     })
#
#     model = Prophet(
#         yearly_seasonality=True,
#         weekly_seasonality=False,
#         daily_seasonality=False,
#         uncertainty_samples=100
#     )
#     model.fit(df)
#
#     last_month = int(data['months'][-1])
#     next_month = last_month + 1 if last_month < 12 else 1
#     future = pd.DataFrame({
#         'ds': [pd.to_datetime(f'2024-{next_month:02d}-01')]
#     })
#
#     forecast = model.predict(future)
#
#     return json.dumps({
#         'predicted': round(forecast['yhat'].iloc[0]),
#         'lower': round(forecast['yhat_lower'].iloc[0]),
#         'upper': round(forecast['yhat_upper'].iloc[0])
#     })