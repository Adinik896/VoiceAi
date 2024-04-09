import g4f

def getResponse(query):
     response = g4f.ChatCompletion.create(
            model="gpt-4-32k-0613",
            provider=g4f.Provider.Bing,
            messages=[{"role":"user","content":query}],
            stream=False,
     )
     return response
